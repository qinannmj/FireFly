package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionSuccessMessage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.state.ClusterState;

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
