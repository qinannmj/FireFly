package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.ElectionEventListener;
import cn.com.sparkle.firefly.event.listeners.EventListener;

public class ElectionEvent extends AbstractEvent {
	@Override
	public void notifyListener(EventListener listener, Object... args) {
		ElectionEventListener l = (ElectionEventListener) listener;
		l.reElection();
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return ElectionEventListener.class;
	}

	public static void doReElection(EventsManager eventsManager) {
		eventsManager.doEvent(ElectionEventListener.class);
	}

}
