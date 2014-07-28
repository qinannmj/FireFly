package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.AccountBookEventListener;
import cn.com.sparkle.firefly.event.listeners.EventListener;

public final class AccountBookEvent extends AbstractEvent {

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		AccountBookEventListener eventListener = (AccountBookEventListener) listener;
		eventListener.accountInit();
	}

	public static void doInitedEvent(EventsManager eventsManager) {
		eventsManager.doEvent(AccountBookEventListener.class);
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return AccountBookEventListener.class;
	}

}
