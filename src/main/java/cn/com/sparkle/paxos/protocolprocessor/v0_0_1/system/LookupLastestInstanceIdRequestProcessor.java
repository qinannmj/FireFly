package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.LookUpLatestInstanceIdResponse;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.stablestorage.AccountBook;

public class LookupLastestInstanceIdRequestProcessor extends AbstractProtocolV0_0_1Processor {

	//	private final static Logger logger = Logger.getLogger(LookupLastestInstanceIdRequestProcessor.class);
	private AccountBook aBook;

	public LookupLastestInstanceIdRequestProcessor(Context context) {
		super();
		this.aBook = context.getAccountBook();
	}

	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {
		if (messagePackage.hasLookUpLatestInstanceIdRequest()) {
			LookUpLatestInstanceIdResponse response = LookUpLatestInstanceIdResponse.newBuilder().setInstanceId(aBook.getMaxInstanceIdInVote()).build();
			MessagePackage.Builder responseBuilder = MessagePackage.newBuilder();
			responseBuilder.setLookUpLatestInstanceIdResponse(response);
			responseBuilder.setId(messagePackage.getId());
			responseBuilder.setIsLast(true);
			sendResponse(session, responseBuilder.build().toByteArray());
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

}
