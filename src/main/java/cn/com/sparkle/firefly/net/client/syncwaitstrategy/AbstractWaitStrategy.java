package cn.com.sparkle.firefly.net.client.syncwaitstrategy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public abstract class AbstractWaitStrategy implements WaitStrategy {
	private ReentrantLock lock = new ReentrantLock();
	private AtomicInteger count = new AtomicInteger(0);
	private Condition fullConfition = lock.newCondition();

	@Override
	public void fireStrategy(PaxosSession session) throws InterruptedException {
		int c = count.incrementAndGet();
		if (c >= 4) {
			try {
				lock.lock();
				if (count.get() >= 4) {
					fireStrategy(session, fullConfition);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void finishMessageSend() {
		int c = count.decrementAndGet();
		if (c > 4) {
			try {
				lock.lock();
				fullConfition.notify();
			} finally {
				lock.unlock();
			}
		}
	}

	public abstract void fireStrategy(PaxosSession session, Condition fullCondition) throws InterruptedException;

}
