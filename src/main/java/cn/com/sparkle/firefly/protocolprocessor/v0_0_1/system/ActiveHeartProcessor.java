package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.event.events.NodeStateChangeEvent;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ActiveHeartBeatRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.SenatorHeartBeatResponse;
import cn.com.sparkle.firefly.state.NodeState;

public class ActiveHeartProcessor extends AbstractProtocolV0_0_1Processor {

	private Context context;

	public ActiveHeartProcessor(Context context) {
		this.context = context;
	}

	@Override
	public void receive(MessagePackage t, PaxosSession session) throws InterruptedException {
		if (t.hasActiveHeartBeatRequest()) {
			ActiveHeartBeatRequest request = t.getActiveHeartBeatRequest();
			String address = session.get(PaxosSessionKeys.ADDRESS_KEY);
			SenatorHeartBeatResponse heart = request.getHeartBeatResponse();

			NodeState nodeState = new NodeState(request.getAddress());
			nodeState.setLastBeatHeatTime(System.currentTimeMillis());
			nodeState.setMasterConnected(heart.getIsMasterConnected());
			nodeState.setLastElectionId(new ElectionId(heart.getElectionAddress(), heart.getElectionId(), heart.getElectionVersion()));
			nodeState.setLastCanExecuteInstanceId(heart.getLastCanExecuteInstanceId());
			nodeState.setInit(heart.getIsInited());
			nodeState.setConnected(address.equals(request.getAddress()));
			nodeState.setUpToDate(heart.getIsUpToDate());
			nodeState.setMasterDistance(heart.getMasterDistance());
			nodeState.setConnectedValidNode(heart.getConnectedValidNodesList());

			SystemNetNode upLevelNode = context.getcState().getRouteManage().lookupUpLevelNode();
			if (upLevelNode != null) {
				try {
					upLevelNode.sendActiveHeartBeat(nodeState);
				} catch (NetCloseException e) {
				}
			}

			NodeStateChangeEvent.doActiveBeartHeartEvent(context.getEventsManager(), address, nodeState);
		} else {
			super.fireOnReceive(t, session);
		}
	}
}
