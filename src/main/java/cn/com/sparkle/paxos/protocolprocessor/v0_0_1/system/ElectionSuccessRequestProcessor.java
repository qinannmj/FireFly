package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.model.ElectionId;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.ElectionSuccessMessage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.state.ClusterState;

public class ElectionSuccessRequestProcessor extends AbstractProtocolV0_0_1Processor {

	private ClusterState cState;

	public ElectionSuccessRequestProcessor(Context context) {
		super();
		this.cState = context.getcState();
	}

	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {

		if (messagePackage.hasElectionSuccessMessage()) {
			ElectionSuccessMessage message = messagePackage.getElectionSuccessMessage();
			ElectionId id = new ElectionId(message.getId().getId().getAddress(), message.getId().getId().getIncreaseId(), message.getId().getVersion());
			cState.changeLastElectionId(id);
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

}
