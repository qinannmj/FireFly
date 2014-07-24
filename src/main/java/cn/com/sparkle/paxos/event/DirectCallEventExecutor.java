package cn.com.sparkle.paxos.event;

import cn.com.sparkle.paxos.event.events.Event;

public class DirectCallEventExecutor implements EventExecutor {
	@Override
	public void execute(Event event, Object... args) {
		event.notifyAllListener(args);
	}
}
