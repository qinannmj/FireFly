package cn.com.sparkle.paxos.event.events;

import java.util.concurrent.CopyOnWriteArraySet;

import cn.com.sparkle.paxos.event.EventExecutor;
import cn.com.sparkle.paxos.event.listeners.EventListener;

public abstract class AbstractEvent implements Event {
	private CopyOnWriteArraySet<EventListener> eventList = new CopyOnWriteArraySet<EventListener>();
	private EventExecutor eventExecutor;

	@Override
	public final void notifyAllListener(Object... args) {
		for (EventListener listener : eventList) {
			notifyListener(listener, args);
		}
	}

	@Override
	public final void register(EventListener listener) {
		eventList.add(listener);
	}

	@Override
	public final void unRegister(EventListener listener) {
		eventList.remove(listener);
	}

	@Override
	public void setEventExecutor(EventExecutor eventExecutor) {
		this.eventExecutor = eventExecutor;
	}

	@Override
	public void execute(Object... args) {
		eventExecutor.execute(this, args);
	}
}
