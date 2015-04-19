package cn.com.sparkle.firefly.net.client.system.callback;

import java.util.List;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.NodeStateChangeEvent;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;

public class FirstHeartBeatCallBack extends HeartBeatCallBack {
	public FirstHeartBeatCallBack(EventsManager eventsManager) {
		super(eventsManager);
	}

	@Override
	public void call(NetNode nnode,String room, boolean isMasterConnected, long electionIncreaseId, String electionAddress, long electionVersion,
			long lastExecutableInstanceId, boolean isInited, boolean isUptoDate, int masterDistance, List<String> connectedValidNodes) {
		super.call(nnode,room, isMasterConnected, electionIncreaseId, electionAddress, electionVersion, lastExecutableInstanceId, isInited, isUptoDate,
				masterDistance, connectedValidNodes);
		NodeStateChangeEvent.doOpenConnectEvent(getEventsManager(), (SystemNetNode) nnode);
	}

}