package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
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
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;

public class CatchupRequestProcessor extends AbstractProtocolV0_0_1Processor{
	private final static Logger logger = Logger.getLogger(CatchupRequestProcessor.class);

	private final static int MAX_CATCH_SIZE = 1024 * 128;

	private Configuration conf;

	private AccountBook aBook;
	
	private static Executor executor = new ThreadPoolExecutor(3,3,5,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(100));

	public CatchupRequestProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.aBook = context.getAccountBook();
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasCatchUpRequest()) {
			CatchUpRequest request = messagePackage.getCatchUpRequest();
			try{
				executor.execute(makeTask(request,session,messagePackage.getId()));
			}catch(RejectedExecutionException e){
				CatchUpResponse.Builder b = CatchUpResponse.newBuilder();
				MessagePackage.Builder responseBuilder = MessagePackage.newBuilder().setCatchUpResponse(b.build());
				responseBuilder.setId(messagePackage.getId());
				responseBuilder.setIsLast(true);
				sendResponse(session, responseBuilder.build().toByteArray());
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
					if (conf.isDebugLog()) {
						logger.debug("catchup request startId:" + startId + " size:" + size);
					}

					ReadRecordCallback<SuccessfulRecord.Builder> callback = new ReadRecordCallback<StoreModel.SuccessfulRecord.Builder>() {
						int packageSize = 0;

						@Override
						public void read(long instanceId, cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord.Builder successRecord) {
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
							packageSize += recordSize;
							b.addSuccessfulRecords(catchUpRecord);
						}
					};

					aBook.readSuccessRecord(startId, startId + size - 1, new ReadSuccessReadFilter(new SortedReadCallback<SuccessfulRecord.Builder>(callback,
							startId)));
				} catch (IOException e) {
					logger.error("fatal error", e);
				} catch (UnsupportedChecksumAlgorithm e) {
					logger.error("fatal error", e);
				}
				MessagePackage.Builder responseBuilder = MessagePackage.newBuilder().setCatchUpResponse(b.build());
				responseBuilder.setId(msgId);
				responseBuilder.setIsLast(true);
				sendResponse(session, responseBuilder.build().toByteArray());
			}
		};
		
	}
}
