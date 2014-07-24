package cn.com.sparkle.paxos.net.client.syncwaitstrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public class CloseConnectionAfterWaitTimeStrategy extends AbstractWaitStrategy {

	private long waitTime;

	public CloseConnectionAfterWaitTimeStrategy(long waitTime) {
		super();
		this.waitTime = waitTime;
	}

	@Override
	public void fireStrategy(PaxosSession session, Condition fullCondition) throws InterruptedException {
		boolean result = fullCondition.await(waitTime, TimeUnit.MILLISECONDS);
		if (!result) {
			session.closeSession();
		}
	}

}
