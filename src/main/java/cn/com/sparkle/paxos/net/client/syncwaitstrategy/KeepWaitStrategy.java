package cn.com.sparkle.paxos.net.client.syncwaitstrategy;

import java.util.concurrent.locks.Condition;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public class KeepWaitStrategy extends AbstractWaitStrategy {
	@Override
	public void fireStrategy(PaxosSession session, Condition fullCondition) throws InterruptedException {
		fullCondition.await();
	}
}
