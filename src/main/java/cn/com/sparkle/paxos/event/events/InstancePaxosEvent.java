package cn.com.sparkle.paxos.event.events;

//import cn.com.sparkle.paxos.model.Id;
//import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.listeners.EventListener;
import cn.com.sparkle.paxos.event.listeners.InstancePaxosEventListener;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.paxosinstance.InstancePaxosInstance;

public final class InstancePaxosEvent extends AbstractEvent {
	public final static int INSTANCE_SUCCESS = 1;
	public final static int INSTANCE_FAIL = 2;
	public final static int INSTANCE_START = 3;

	public final static void doFailEvent(EventsManager eventsManager, InstancePaxosInstance instance, Id id, long refuseId, Value value) {
		eventsManager.doEvent(InstancePaxosEventListener.class, INSTANCE_FAIL, instance, id, refuseId, value);
	}

	public final static void doSuccessEvent(EventsManager eventsManager, InstancePaxosInstance instance, Value value) {
		eventsManager.doEvent(InstancePaxosEventListener.class, INSTANCE_SUCCESS, instance, value);
	}

	public final static void doStartEvent(EventsManager eventsManager, InstancePaxosInstance instance) {
		eventsManager.doEvent(InstancePaxosEventListener.class, INSTANCE_START, instance);
	}

	@Override
	public void notifyListener(EventListener listener, Object... args) {
		InstancePaxosEventListener eventListener = (InstancePaxosEventListener) listener;
		int type = (Integer) args[0];
		switch (type) {
		case INSTANCE_SUCCESS:
			eventListener.instanceSuccess((InstancePaxosInstance) args[1], (Value) args[2]);
			break;
		case INSTANCE_FAIL:
			eventListener.instanceFail((InstancePaxosInstance) args[1], (Id) args[2], (Long) args[3], (Value) args[4]);
			break;
		case INSTANCE_START:
			eventListener.instanceStart((InstancePaxosInstance) args[1]);
			break;
		default:
			throw new RuntimeException("not supported argument:" + type);
		}

	}

	@Override
	public Class<? extends EventListener> matchListener() {
		return InstancePaxosEventListener.class;
	}

}
