package cn.com.sparkle.paxos.event;

import cn.com.sparkle.paxos.event.events.Event;

public interface EventExecutor {
	public void execute(Event event, Object... args);
}
