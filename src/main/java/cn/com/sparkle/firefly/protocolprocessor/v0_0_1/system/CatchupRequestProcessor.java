package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.protobuf.ByteString;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.listeners.MasterChangePosEventListener;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRecord;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.CatchUpResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.ReadRecordCallback;
import cn.com.sparkle.firefly.stablestorage.ReadSuccessReadFilter;
import cn.com.sparkle.firefly.stablestorage.SortedReadCallback;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Id;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.firefly.util.LongUtil;

public class CatchupRequestProcessor extends AbstractProtocolV0_0_1Processor implements MasterChangePosEventListener{
	private final static Logger logger = Logger.getLogger(CatchupRequestProcessor.class);

	private final static int MAX_CATCH_SIZE = 1024 * 1024;

	private Configuration conf;

	private AccountBook aBook;
	
	private volatile boolean isMaster = false;
	
	private static Executor executor = new ThreadPoolExecutor(3,3,5,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(100));

	public CatchupRequestProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.aBook = context.getAccountBook();
		context.getEventsManager().registerListener(this);
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasCatchUpRequest()) {
			CatchUpRequest request = messagePackage.getCatchUpRequest();
			
			try{
				if(isMaster || !request.hasIsArbitrator() || !request.getIsArbitrator()){
					executor.execute(makeTask(request,session,messagePackage.getId()));
				}else{
					
					CatchUpResponse.Builder b = CatchUpResponse.newBuilder();
					if(aBook.getLastCanExecutableInstanceId() > request.getStartInstanceId()){
						byte[] buff = new byte[8];
						LongUtil.toByte(aBook.getLastCanExecutableInstanceId(), buff, 0);
						Value.Builder value = Value.newBuilder().setValues(ByteString.copyFrom(buff)).setType(ValueType.PLACE.getValue());
						SuccessfulRecord.Builder successRecord = SuccessfulRecord.newBuilder().setHighestVoteNum(Id.newBuilder().setAddress("").setIncreaseId(0)).setV(value);
						CatchUpRecord.Builder record = CatchUpRecord.newBuilder().setInstanceId(request.getStartInstanceId()).setValue(successRecord);
						b.addSuccessfulRecords(record);
					}
					sendLastPackage(session,b.build(),messagePackage.getId());
				}
			}catch(RejectedExecutionException e){
				CatchUpResponse.Builder b = CatchUpResponse.newBuilder();
				sendLastPackage(session,b.build(),messagePackage.getId());
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}
	private Runnable makeTask(final CatchUpRequest request,final PaxosSession session,final long msgId){
		
		return new Runnable() {
			long startId = request.getStartInstanceId();
			int size = request.getSize();
			@Override
			public void run() {
				
				final CatchUpResponse.Builder b = CatchUpResponse.newBuilder();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("catchup request startId:" + startId + " size:" + size);
					}
					
					ReadRecordCallback<SuccessfulRecord.Builder> callback = new ReadRecordCallback<StoreModel.SuccessfulRecord.Builder>() {
						int packageSize = 0;
						boolean isStop = false;
						@Override
						public void read(long instanceId, cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord.Builder successRecord) {
							if(isStop){
								return;
							}
							SuccessfulRecordWrap successfulRecordWrap = new SuccessfulRecordWrap(instanceId, successRecord.build(), null);
							CatchUpRecord catchUpRecord = CatchUpRecord.newBuilder().setInstanceId(successfulRecordWrap.getInstanceId())
									.setValue(successfulRecordWrap.getRecord()).build();
							
							int recordSize = catchUpRecord.getSerializedSize();
							if (packageSize + recordSize >= MAX_CATCH_SIZE && b.getSuccessfulRecordsCount() != 0) {
								MessagePackage.Builder response = MessagePackage.newBuilder().setCatchUpResponse(b.build());
								response.setId(msgId);
								response.setIsLast(false);
								sendResponse(session, response.build().toByteArray());
								b.clear();
								packageSize = 0;
							}
							if(successRecord.getV().getType() == ValueType.PLACE.getValue()){
								//for arbitrator
								isStop = true;
							}else{
								packageSize += recordSize;
								b.addSuccessfulRecords(catchUpRecord);
							}
						}
					};
					
					aBook.readSuccessRecord(startId, startId + size - 1, new ReadSuccessReadFilter(new SortedReadCallback<SuccessfulRecord.Builder>(callback,
							startId)));
				} catch (IOException e) {
					logger.error("fatal error", e);
				} catch (UnsupportedChecksumAlgorithm e) {
					logger.error("fatal error", e);
				} catch (Throwable e){
					logger.error("fatal error", e);
				}
				sendLastPackage(session,b.build(),msgId);
			}
		};
		
	}
	
	private void sendLastPackage(PaxosSession session,CatchUpResponse response,long msgId){
		MessagePackage.Builder responseBuilder = MessagePackage.newBuilder().setCatchUpResponse(response);
		responseBuilder.setId(msgId);
		responseBuilder.setIsLast(true);
		sendResponse(session, responseBuilder.build().toByteArray());
		if(logger.isDebugEnabled()){
			logger.debug("proceeded a catch request");
		}
	}
	
	@Override
	public void getMasterPos() {
		isMaster = true;
	}

	@Override
	public void lostPos() {
		isMaster = false;
	}

	@Override
	public void masterChange(String address) {
		// TODO Auto-generated method stub
		
	}
}
