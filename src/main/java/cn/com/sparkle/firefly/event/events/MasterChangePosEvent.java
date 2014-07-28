package cn.com.sparkle.firefly.event.events;

import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.EventListener;
import cn.com.sparkle.firefly.event.listeners.MasterChangePosEventListener;

public final class MasterChangePosEvent extends AbstractEvent {
	public final static int GET_POS_MASTER = 1;
	public final static int LOST_POS_MASTER = 2;
	public final static int MASTER_CHANGE = 3;

	public final static void doGetMasterPosEvent(EventsManager eventsManager) {
		eventsManager.doEvent(MasterChangePosEventListener.class, GET_POS_MASTER);
	}

	public final static void doLostPosEvent(EventsManager eventsManager) {
		eventsManager.doEvent(MasterChangePosEventListener.class, LOST_POS_MASTER);
	}

	public final static void doMasterChangeEvent(EventsManager eventsManager, String address) {
		eventsManager.doEvent(MasterChangePosEventListener.class, MASTER_CHANGE, address);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		MasterChangePosEventListener eventListener = (MasterChangePosEventListener) listener;
		int type = (Integer) args[0];
		switch (type) {
		case GET_POS_MASTER:
			eventListener.getMasterPos();
			break;
		case LOST_POS_MASTER:
			eventListener.lostPos();
			break;
		case MASTER_CHANGE:
			eventListener.masterChange((String) args[1]);
		}
	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return MasterChangePosEventListener.class;
	}

}
