package cn.com.sparkle.paxos.event.events;

import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.listeners.EventListener;
import cn.com.sparkle.paxos.event.listeners.SpeedControlEventListener;

public final class SpeedControlEvent extends AbstractEvent {
	public final static int SUGGEST_MAX_PACKAGE_SIZE = 1;

	public final static void doSuggestMaxPackageSizeEvent(EventsManager eventsManager, int suggestSize) {
		eventsManager.doEvent(SpeedControlEventListener.class, SUGGEST_MAX_PACKAGE_SIZE, suggestSize);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		SpeedControlEventListener eventListener = (SpeedControlEventListener) listener;
		int type = (Integer) args[0];
		switch (type) {
		case SUGGEST_MAX_PACKAGE_SIZE:
			eventListener.suggestMaxPackageSize((Integer) args[1]);
			break;
		default:
			throw new RuntimeException("not supported argument:" + type);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return SpeedControlEventListener.class;
	}

}
