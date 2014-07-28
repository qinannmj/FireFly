package cn.com.sparkle.firefly;


import cn.com.sparkle.firefly.event.DefaultEventManager;
import cn.com.sparkle.firefly.event.events.CatchUpEvent;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;

public class TestEventManager implements CatchUpEventListener{
	public static void main(String[] args) throws InstantiationException, IllegalAccessException {
		DefaultEventManager eventManager = new DefaultEventManager();
		TestEventManager instance = new TestEventManager();
		eventManager.registerListener(instance);
		CatchUpEvent.doCatchUpFailEvent(eventManager);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void catchUpFail() {
		System.out.println(Thread.currentThread() + "catch up");
	}

	@Override
	public void recoveryFromFail() {
		System.out.println("fail catchup");
	}
}
