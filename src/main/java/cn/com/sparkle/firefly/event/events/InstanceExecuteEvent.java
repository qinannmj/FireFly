package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.InstanceExecuteEventListener;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;

public final class InstanceExecuteEvent extends AbstractEvent {
	public final static int EVENT_FINISH_EXECUTE = 1;

	public static void doEventExecutedEvent(EventsManager eventsManager, SuccessfulRecord record) {
		eventsManager.doEvent(InstanceExecuteEventListener.class, EVENT_FINISH_EXECUTE, record);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		InstanceExecuteEventListener eventListener = (InstanceExecuteEventListener) listener;
		int type = (Integer) args[0];
		switch (type) {
		case EVENT_FINISH_EXECUTE:
			eventListener.instanceExecuted((SuccessfulRecord) args[1]);
			break;
		default:
			throw new RuntimeException("not supported argument:" + type);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return InstanceExecuteEventListener.class;
	}

}
