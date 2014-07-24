package cn.com.sparkle.paxos.event.events;

import java.util.Set;

import cn.com.sparkle.paxos.config.ConfigNode;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.listeners.ConfigureEventListener;
import cn.com.sparkle.paxos.event.listeners.EventListener;

public final class ConfigureEvent extends AbstractEvent {
	public static enum Op{
		ADD,REMOVE
	}
	
	public final static int SENATORS_CHANGE = 2;
	
	public final static void doSenatorsChangeEvent(EventsManager eventsManager, Set<ConfigNode> newSenators,ConfigNode configNode ,Op op , long version) {
		eventsManager.doEvent(ConfigureEventListener.class, SENATORS_CHANGE, newSenators, version , configNode,op);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void notifyListener(EventListener listener, Object... args) {
		int i = (Integer) args[0];
		ConfigureEventListener eventListener = (ConfigureEventListener) listener;
		switch (i) {
		case SENATORS_CHANGE:
			
			ConfigNode node = (ConfigNode)args[3];
			if(args[4] == Op.ADD){
				eventListener.senatorsChange((Set<ConfigNode>) args[1],node,null, (Long) args[2]);
			}else{
				eventListener.senatorsChange((Set<ConfigNode>) args[1],null,node, (Long) args[2]);
			}
			break;
		default:
			throw new RuntimeException("not support event type :" + i);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return ConfigureEventListener.class;
	}
}
