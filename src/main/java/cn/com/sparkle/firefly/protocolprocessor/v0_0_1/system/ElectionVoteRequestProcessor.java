package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionVoteRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.VoteResponse;
import cn.com.sparkle.firefly.state.ClusterState;

public class ElectionVoteRequestProcessor extends AbstractProtocolV0_0_1Processor {

	//	private final static Logger logger = Logger.getLogger(ElectionVoteRequestProcessor.class);
	private ClusterState cState;

	public ElectionVoteRequestProcessor(Context context) {
		super();
		this.cState = context.getcState();
	}

	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {
		if (messagePackage.hasElectionVoteRequest()) {
			ElectionVoteRequest request = messagePackage.getElectionVoteRequest();
			ElectionId id = new ElectionId(request.getId().getId().getAddress(), request.getId().getId().getIncreaseId(), request.getId().getVersion());

			ElectionId returnId = cState.getSelfState().casElectionPrepareId(id, request.getLastVoteId());
			VoteResponse response;
			if (id == returnId) {
				response = VoteResponse.newBuilder().setRefuseId(Constants.VOTE_OK).build();
			} else {
				response = VoteResponse.newBuilder().setRefuseId(id.getIncreaseId()).build();
			}
			MessagePackage.Builder responseBuilder = MessagePackage.newBuilder();
			responseBuilder.setVoteResponse(response);
			responseBuilder.setId(messagePackage.getId());
			responseBuilder.setIsLast(true);
			sendResponse(session, responseBuilder.build());
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

}
