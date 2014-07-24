package cn.com.sparkle.paxos.event.events;

import cn.com.sparkle.paxos.event.EventExecutor;
import cn.com.sparkle.paxos.event.listeners.EventListener;

public interface Event {
	public void register(EventListener listener);

	public void unRegister(EventListener listener);

	public void notifyAllListener(Object... args);

	public void setEventExecutor(EventExecutor eventExecutor);

	public void execute(Object... args);

	public void notifyListener(EventListener listener, Object... args);

	public Class<? extends EventListener> matchListener();
}
