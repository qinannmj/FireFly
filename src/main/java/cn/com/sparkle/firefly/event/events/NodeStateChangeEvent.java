package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.NodeStateChangeEventListener;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.state.NodeState;

public final class NodeStateChangeEvent extends AbstractEvent {
	public final static int LOSE_CONNECT = 1;
	public final static int OPEN_CONNECT = 2;
	public final static int BEAT_HEART = 3;
	public final static int ACTVIE_HEART = 4;

	public final static void doLoseConnectEvent(EventsManager eventsManager, NetNode nNode) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, LOSE_CONNECT, nNode);
	}

	public final static void doOpenConnectEvent(EventsManager eventsManager, NetNode nNode) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, OPEN_CONNECT, nNode);
	}

	public final static void doBeatHeartEvent(EventsManager eventsManager, NetNode nNode, NodeState nState) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, BEAT_HEART, nNode, nState);
	}

	public final static void doActiveBeartHeartEvent(EventsManager eventsManager, String fromNode, NodeState nState) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, ACTVIE_HEART, fromNode, nState);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		NodeStateChangeEventListener eventListener = (NodeStateChangeEventListener) listener;
		int s = (Integer) args[0];
		switch (s) {
		case LOSE_CONNECT:
			eventListener.loseConnect((NetNode) args[1]);
			break;
		case OPEN_CONNECT:
			eventListener.openConnect((NetNode) args[1]);
			break;
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
		return NodeStateChangeEventListener.class;
	}

}
