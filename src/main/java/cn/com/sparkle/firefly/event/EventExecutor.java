package cn.com.sparkle.firefly.event;

import cn.com.sparkle.firefly.event.events.Event;

public interface EventExecutor {
	public void execute(Event event, Object... args);
}
