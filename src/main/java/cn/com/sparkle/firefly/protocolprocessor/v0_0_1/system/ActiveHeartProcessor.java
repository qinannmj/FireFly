package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.event.events.HeartBeatEvent;
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
import cn.com.sparkle.raptor.core.util.TimeUtil;

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
			boolean isArbitrator = false;
			if(request.hasIsArbitrator()){
				isArbitrator = request.getIsArbitrator();
			}

			NodeState nodeState = new NodeState(request.getAddress());
			nodeState.setRoom(heart.getRoom());
			nodeState.setArbitrator(isArbitrator);
			nodeState.setLastBeatHeatTime(TimeUtil.currentTimeMillis());
			nodeState.setMasterConnected(heart.getIsMasterConnected());
			nodeState.setLastElectionId(new ElectionId(heart.getElectionAddress(), heart.getElectionId(), heart.getElectionVersion()));
			nodeState.setLastCanExecuteInstanceId(heart.getLastCanExecuteInstanceId());
			nodeState.setInit(heart.getIsInited());
			nodeState.setConnected(address.equals(request.getAddress()));
			nodeState.setUpToDate(heart.getIsUpToDate());
			nodeState.setMasterDistance(heart.getMasterDistance());
			nodeState.setConnectedValidNode(heart.getConnectedValidNodesList());

			SystemNetNode upLevelNode = context.getcState().getRouteManage().lookupUpLevelNode();
			if (upLevelNode != null && request.getLifecycle() > 0) {
				try {
					upLevelNode.sendActiveHeartBeat(nodeState,request.getLifecycle() - 1);
				} catch (NetCloseException e) {
				}
			}

			HeartBeatEvent.doActiveBeartHeartEvent(context.getEventsManager(), address, nodeState);
		} else {
			super.fireOnReceive(t, session);
		}
	}
}
