package cn.com.sparkle.firefly.event;

import java.util.HashMap;

import cn.com.sparkle.firefly.event.events.AccountBookEvent;
import cn.com.sparkle.firefly.event.events.CatchUpEvent;
import cn.com.sparkle.firefly.event.events.ConfigureEvent;
import cn.com.sparkle.firefly.event.events.ElectionEvent;
import cn.com.sparkle.firefly.event.events.Event;
import cn.com.sparkle.firefly.event.events.ExecutingInstanceChangeEvent;
import cn.com.sparkle.firefly.event.events.HeartBeatEvent;
import cn.com.sparkle.firefly.event.events.InstanceExecuteEvent;
import cn.com.sparkle.firefly.event.events.InstanceExecuteMaxPackageSizeEvent;
import cn.com.sparkle.firefly.event.events.InstancePaxosEvent;
import cn.com.sparkle.firefly.event.events.MasterChangePosEvent;
import cn.com.sparkle.firefly.event.events.MasterDistanceChangeEvent;
import cn.com.sparkle.firefly.event.events.NodeStateChangeEvent;
import cn.com.sparkle.firefly.event.events.SpeedControlEvent;
import cn.com.sparkle.firefly.event.listeners.EventListener;

public class DefaultEventManager implements EventsManager {

	private HashMap<Class<? extends EventListener>, Event> eventMap = new HashMap<Class<? extends EventListener>, Event>();

	public DefaultEventManager() throws InstantiationException, IllegalAccessException {
		register(InstancePaxosEvent.class, new DisruptorEventExecutor());
		
		DisruptorEventExecutor executor = new DisruptorEventExecutor();
		register(MasterChangePosEvent.class, executor);
		register(CatchUpEvent.class, executor);
		register(ExecutingInstanceChangeEvent.class, executor);
		register(ConfigureEvent.class, executor);
		register(NodeStateChangeEvent.class, executor);
		register(HeartBeatEvent.class,executor);
		register(InstanceExecuteEvent.class, executor);
		register(MasterDistanceChangeEvent.class, executor);
		register(ElectionEvent.class, executor);

		register(InstanceExecuteMaxPackageSizeEvent.class, new DirectCallEventExecutor());
		register(SpeedControlEvent.class, new DirectCallEventExecutor());
		register(AccountBookEvent.class, new DirectCallEventExecutor());

	}

	public void doEvent(Class<? extends EventListener> clazz, Object... args) {
		Event event = eventMap.get(clazz);
		if (event == null) {
			throw new RuntimeException("unsupported event");
		} else {
			event.execute(args);
		}
	}

	public void register(Class<? extends Event> clazz, EventExecutor executor) throws InstantiationException, IllegalAccessException {
		Event event;
		try {
			event = clazz.newInstance();
		} catch (InstantiationException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		}
		event.setEventExecutor(executor);
		eventMap.put(event.matchListener(), event);
	}

	public void unregister(Class<? extends Event> clazz) {
		eventMap.remove(clazz);
	}

	@SuppressWarnings("rawtypes")
	public void registerListener(EventListener listener) {
		Class[] clazzs = listener.getClass().getInterfaces();
		for (Class clazz : clazzs) {
			Event event = eventMap.get(clazz);
			if (event != null) {
				event.register(listener);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void unregisterListener(EventListener listener) {
		Class[] clazzs = listener.getClass().getInterfaces();
		for (Class clazz : clazzs) {
			Event event = eventMap.get(clazz);
			if (event != null) {
				event.unRegister(listener);
			}
		}
	}

	@Override
	public void unRegisterListener(Class<? extends EventListener> clazz, EventListener listener) {
		Event event = eventMap.get(clazz);
		if (event != null) {
			event.unRegister(listener);
		}
	}

	@Override
	public void registerListener(Class<? extends EventListener> clazz, EventListener listener) {
		Event event = eventMap.get(clazz);
		if (event != null) {
			event.register(listener);
		}
	}
}
