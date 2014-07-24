package cn.com.sparkle.paxos.event.events;

import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.listeners.EventListener;
import cn.com.sparkle.paxos.event.listeners.MasterDistanceChangeListener;

public class MasterDistanceChangeEvent extends AbstractEvent {

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		MasterDistanceChangeListener l = (MasterDistanceChangeListener) listener;
		l.masterDistanceChange((Integer) args[0]);
	}

	public static void masterDistanceChange(EventsManager eventsManager, int distance) {
		eventsManager.doEvent(MasterDistanceChangeListener.class, distance);
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return MasterDistanceChangeListener.class;
	}
}
