package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.ExecutingInstanceChangeEventListener;
import cn.com.sparkle.firefly.paxosinstance.InstancePaxosInstance;

public final class ExecutingInstanceChangeEvent extends AbstractEvent {
	private final static int ENTRY_EXECUTION = 1;
	private final static int EXIT_EXECUTION = 2;

	public final static void doEntryExecutionEvent(EventsManager eventsManager, InstancePaxosInstance instance) {
		eventsManager.doEvent(ExecutingInstanceChangeEventListener.class, ENTRY_EXECUTION, instance);
	}

	public final static void doExitExecutionEvent(EventsManager eventsManager, InstancePaxosInstance instance) {
		eventsManager.doEvent(ExecutingInstanceChangeEventListener.class, EXIT_EXECUTION, instance);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		int i = (Integer) args[0];
		ExecutingInstanceChangeEventListener eventListener = (ExecutingInstanceChangeEventListener) listener;
		switch (i) {
		case ENTRY_EXECUTION:
			eventListener.entryExecution((InstancePaxosInstance) args[1]);
			break;
		case EXIT_EXECUTION:
			eventListener.exitExecution((InstancePaxosInstance) args[1]);
			break;
		}

	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return ExecutingInstanceChangeEventListener.class;
	}
}
