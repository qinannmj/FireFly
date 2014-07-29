package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.NodeStateChangeEventListener;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.state.NodeState;

public final class NodeStateChangeEvent extends AbstractEvent {
	public final static int LOSE_CONNECT = 1;
	public final static int OPEN_CONNECT = 2;
	public final static int NODESTATE_CHANGE = 3;

	public final static void doLoseConnectEvent(EventsManager eventsManager, NetNode nNode) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, LOSE_CONNECT, nNode);
	}

	public final static void doOpenConnectEvent(EventsManager eventsManager, NetNode nNode) {
		eventsManager.doEvent(NodeStateChangeEventListener.class, OPEN_CONNECT, nNode);
	}

	public final static void doNodeStateChange(EventsManager eventsManager , String fromNodeAddress , NodeState nState1,NodeState nState2){
		eventsManager.doEvent(NodeStateChangeEventListener.class,NODESTATE_CHANGE, fromNodeAddress,nState1,nState2);
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
		case NODESTATE_CHANGE:
			eventListener.nodeStateChange((String)args[1], (NodeState)args[2], (NodeState)args[3]);
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
