package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.event.events.ElectionEvent;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.ReAssignElectionRequest;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.ReAssignElectionResponse;

public class AssignSelfElectionProcessor extends AbstractProtocolV0_0_1Processor {
	private Context context;

	public AssignSelfElectionProcessor(Context context) {
		super();
		this.context = context;
	}

	@Override
	public void receive(MessagePackage t, PaxosSession session) throws InterruptedException {
		if (t.hasReAssignElectionRequest()) {
			ReAssignElectionRequest request = t.getReAssignElectionRequest();
			if (request.getNodeId().equals(context.getConfiguration().getSelfAddress())) {
				ElectionEvent.doReElection(context.getEventsManager());

				ReAssignElectionResponse response = ReAssignElectionResponse.getDefaultInstance();
				MessagePackage.Builder mp = MessagePackage.newBuilder().setReAssignElectionResponse(response);
				mp.setId(t.getId());
				mp.setIsLast(true);
				sendResponse(session, mp.build().toByteArray());
			} else {
				throw new RuntimeException("The nodeId is not belonged to this node!");
			}
		} else {
			super.fireOnReceive(t, session);
		}

	}
}
