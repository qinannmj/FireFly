package cn.com.sparkle.firefly.net.client.syncwaitstrategy;

import java.util.concurrent.locks.Condition;

import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public class KeepWaitStrategy extends AbstractWaitStrategy {
	@Override
	public void fireStrategy(PaxosSession session, Condition fullCondition) throws InterruptedException {
		fullCondition.await();
	}
}
