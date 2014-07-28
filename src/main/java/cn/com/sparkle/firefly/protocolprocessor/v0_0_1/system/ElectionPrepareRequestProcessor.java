package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionPrepareRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseBad;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseGood;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.firefly.state.ClusterState;

public class ElectionPrepareRequestProcessor extends AbstractProtocolV0_0_1Processor {

	//	private final static Logger logger = Logger.getLogger(ElectionPrepareRequestProcessor.class);
	private ClusterState cState;

	public ElectionPrepareRequestProcessor(Context context) {
		super();
		this.cState = context.getcState();
	}

	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {
		if (messagePackage.hasElectionPrepareRequest()) {
			ElectionPrepareRequest request = messagePackage.getElectionPrepareRequest();
			ElectionId id = new ElectionId(request.getId().getId().getAddress(), request.getId().getId().getIncreaseId(), request.getId().getVersion());
			ElectionId returnId = cState.getSelfState().casElectionPrepareId(id, request.getLastVoteId());
			MessagePackage.Builder responseBuilder = MessagePackage.newBuilder();

			if (id == returnId) {
				Value t = Value.newBuilder().setType(ValueType.ADMIN.getValue()).build();
				PrepareResponseGood response = PrepareResponseGood.newBuilder().setLastVotedId(request.getId().getId()).setValue(t).build();
				responseBuilder.setPrepareResponseGood(response);
			} else {
				PrepareResponseBad response = PrepareResponseBad.newBuilder().setLastPrepareId(id.getIncreaseId()).build();
				responseBuilder.setPrepareResponseBad(response);
			}

			//send response
			responseBuilder.setId(messagePackage.getId());
			responseBuilder.setIsLast(true);
			sendResponse(session, responseBuilder.build().toByteArray());
		} else {
			super.fireOnReceive(messagePackage, session);
		}

	}
}
