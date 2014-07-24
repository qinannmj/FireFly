package cn.com.sparkle.paxos.event;

import cn.com.sparkle.paxos.event.events.Event;
import cn.com.sparkle.paxos.event.listeners.EventListener;

public interface EventsManager {
	public void register(Class<? extends Event> clazz, EventExecutor executor) throws InstantiationException, IllegalAccessException;

	public void unregister(Class<? extends Event> clazz);

	public void registerListener(EventListener listener);

	public void registerListener(Class<? extends EventListener> clazz, EventListener listener);

	public void unregisterListener(EventListener listener);

	public void unRegisterListener(Class<? extends EventListener> clazz, EventListener listener);

	public void doEvent(Class<? extends EventListener> clazz, Object... args);
}
