package cn.com.sparkle.paxos.event.events;

import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.listeners.EventListener;
import cn.com.sparkle.paxos.event.listeners.InstanceExecuteMaxPackageSizeEventListener;

public final class InstanceExecuteMaxPackageSizeEvent extends AbstractEvent {
	public final static int MAX_PACKAGE_SIZE_CHANGE = 1;

	public final static void doMaxPackageSizeChangeEvent(EventsManager eventsManager, int curMaxPackageSize) {
		eventsManager.doEvent(InstanceExecuteMaxPackageSizeEventListener.class, MAX_PACKAGE_SIZE_CHANGE, curMaxPackageSize);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		InstanceExecuteMaxPackageSizeEventListener eventListener = (InstanceExecuteMaxPackageSizeEventListener) listener;
		int type = (Integer) args[0];
		switch (type) {
		case MAX_PACKAGE_SIZE_CHANGE:
			eventListener.maxPackageSizeChange((Integer) args[1]);
			break;
		default:
			throw new RuntimeException("not supported argument:" + type);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return InstanceExecuteMaxPackageSizeEventListener.class;
	}
}
