package cn.com.sparkle.firefly.event.events;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;
import cn.com.sparkle.firefly.event.listeners.EventListener;

public final class CatchUpEvent extends AbstractEvent {

	private final static Logger logger = Logger.getLogger(CatchUpEvent.class);

	public final static int FAIL_CATCH_UP = 1;
	public final static int RECOVERY_FROM_CATCH_UP = 2;

	public final static void doCatchUpFailEvent(EventsManager eventsManager) {
		eventsManager.doEvent(CatchUpEventListener.class, FAIL_CATCH_UP);
	}

	public final static void doRecoveryFromFailEvent(EventsManager eventsManager) {
		eventsManager.doEvent(CatchUpEventListener.class, RECOVERY_FROM_CATCH_UP);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		CatchUpEventListener eventListener = (CatchUpEventListener) listener;
		switch ((Integer) args[0]) {
		case CatchUpEvent.FAIL_CATCH_UP:
			eventListener.catchUpFail();
			break;
		case CatchUpEvent.RECOVERY_FROM_CATCH_UP:
			eventListener.recoveryFromFail();
			break;
		default:
			logger.error("Not support event argument!");
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return CatchUpEventListener.class;
	}
}
