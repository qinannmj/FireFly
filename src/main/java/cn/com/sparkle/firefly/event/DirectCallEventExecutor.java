package cn.com.sparkle.firefly.event;

import cn.com.sparkle.firefly.event.events.Event;

public class DirectCallEventExecutor implements EventExecutor {
	@Override
	public void execute(Event event, Object... args) {
		event.notifyAllListener(args);
	}
}
