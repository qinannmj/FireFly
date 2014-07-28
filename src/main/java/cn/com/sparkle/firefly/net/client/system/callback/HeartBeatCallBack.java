package cn.com.sparkle.firefly.net.client.system.callback;

import java.util.List;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.NodeStateChangeEvent;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.state.NodeState;

public class HeartBeatCallBack {
	private EventsManager eventsManager;

	public HeartBeatCallBack(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	protected EventsManager getEventsManager() {
		return eventsManager;
	}

	public void call(NetNode nnode, boolean isMasterConnected, long electionIncreaseId, String electionAddress, long electionVersion,
			long lastExecutableInstanceId, boolean isInited, boolean isUptoDate, int masterDistance, List<String> connectedValidNodes) {
		NodeState nodeState = new NodeState(nnode.getAddress());
		nodeState.setLastBeatHeatTime(System.currentTimeMillis());
		nodeState.setMasterConnected(isMasterConnected);
		nodeState.setLastElectionId(new ElectionId(electionAddress, electionIncreaseId, electionVersion));
		nodeState.setLastCanExecuteInstanceId(lastExecutableInstanceId);
		nodeState.setInit(isInited);
		nodeState.setConnected(!nnode.isClose());
		nodeState.setUpToDate(isUptoDate);
		nodeState.setMasterDistance(masterDistance);
		nodeState.setConnectedValidNode(connectedValidNodes);
		NodeStateChangeEvent.doBeatHeartEvent(eventsManager, (SystemNetNode) nnode, nodeState);
	}
}