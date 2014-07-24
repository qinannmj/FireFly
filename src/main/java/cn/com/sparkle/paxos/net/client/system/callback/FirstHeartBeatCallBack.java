package cn.com.sparkle.paxos.net.client.system.callback;

import java.util.List;

import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.events.NodeStateChangeEvent;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.system.SystemNetNode;

public class FirstHeartBeatCallBack extends HeartBeatCallBack {
	public FirstHeartBeatCallBack(EventsManager eventsManager) {
		super(eventsManager);
	}

	@Override
	public void call(NetNode nnode, boolean isMasterConnected, long electionIncreaseId, String electionAddress, long electionVersion,
			long lastExecutableInstanceId, boolean isInited, boolean isUptoDate, int masterDistance, List<String> connectedValidNodes) {
		super.call(nnode, isMasterConnected, electionIncreaseId, electionAddress, electionVersion, lastExecutableInstanceId, isInited, isUptoDate,
				masterDistance, connectedValidNodes);
		NodeStateChangeEvent.doOpenConnectEvent(getEventsManager(), (SystemNetNode) nnode);
	}

}