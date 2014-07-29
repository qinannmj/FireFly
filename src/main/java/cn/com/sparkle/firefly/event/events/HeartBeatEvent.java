package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.HeartBeatEventListener;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.state.NodeState;

public final class HeartBeatEvent extends AbstractEvent {
	public final static int BEAT_HEART = 1;
	public final static int ACTVIE_HEART = 2;


	public final static void doBeatHeartEvent(EventsManager eventsManager, NetNode nNode, NodeState nState) {
		eventsManager.doEvent(HeartBeatEventListener.class, BEAT_HEART, nNode, nState);
	}

	public final static void doActiveBeartHeartEvent(EventsManager eventsManager, String fromNode, NodeState nState) {
		eventsManager.doEvent(HeartBeatEventListener.class, ACTVIE_HEART, fromNode, nState);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		HeartBeatEventListener eventListener = (HeartBeatEventListener) listener;
		int s = (Integer) args[0];
		switch (s) {
		case BEAT_HEART:
			eventListener.beatHeart((NetNode) args[1], (NodeState) args[2]);
			break;
		case ACTVIE_HEART:
			eventListener.activeBeatHeart((String) args[1], (NodeState) args[2]);
			break;
		default:
			throw new RuntimeException("not supported arguments :" + s);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return HeartBeatEventListener.class;
	}

}
