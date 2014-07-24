package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRecord;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRequest;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.CatchUpResponse;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.stablestorage.ReadSuccessRecordCallback;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.SuccessfulRecordWrap;

public class CatchupRequestProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(CatchupRequestProcessor.class);

	private final static int MAX_CATCH_SIZE = 1024 * 128;

	private Configuration conf;

	private AccountBook aBook;

	public CatchupRequestProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.aBook = context.getAccountBook();
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasCatchUpRequest()) {
			CatchUpRequest request = messagePackage.getCatchUpRequest();
			long startId = request.getStartInstanceId();
			int size = request.getSize();
			final CatchUpResponse.Builder b = CatchUpResponse.newBuilder();
			try {
				if (conf.isDebugLog()) {
					logger.debug("catchup request startId:" + startId + " size:" + size);
				}
				aBook.readSuccessRecord(startId, startId + size - 1, new ReadSuccessRecordCallback() {
					int packageSize = 0;

					@Override
					public void readSuccess(long instanceId, SuccessfulRecord.Builder successRecord) {
						SuccessfulRecordWrap successfulRecordWrap = new SuccessfulRecordWrap(instanceId, successRecord.build(), null);
						CatchUpRecord catchUpRecord = CatchUpRecord.newBuilder().setInstanceId(successfulRecordWrap.getInstanceId())
								.setValue(successfulRecordWrap.getRecord()).build();
						int recordSize = catchUpRecord.getSerializedSize();
						if (packageSize + recordSize >= MAX_CATCH_SIZE && b.getSuccessfulRecordsCount() != 0) {
							MessagePackage.Builder response = MessagePackage.newBuilder().setCatchUpResponse(b.build());
							response.setId(messagePackage.getId());
							response.setIsLast(false);
							sendResponse(session, response.build().toByteArray());
							b.clear();
							packageSize = 0;
						}
						packageSize += recordSize;
						b.addSuccessfulRecords(catchUpRecord);
					}
				});
			} catch (IOException e) {
				logger.error("fatal error", e);
			} catch (UnsupportedChecksumAlgorithm e) {
				logger.error("fatal error", e);
			}
			MessagePackage.Builder responseBuilder = MessagePackage.newBuilder().setCatchUpResponse(b.build());
			responseBuilder.setId(messagePackage.getId());
			responseBuilder.setIsLast(true);
			sendResponse(session, responseBuilder.build().toByteArray());
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

}
