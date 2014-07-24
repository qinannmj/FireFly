package cn.com.sparkle.paxos.net.client.syncwaitstrategy;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public interface WaitStrategy {
	public void fireStrategy(PaxosSession session) throws InterruptedException;

	public void finishMessageSend();
}
