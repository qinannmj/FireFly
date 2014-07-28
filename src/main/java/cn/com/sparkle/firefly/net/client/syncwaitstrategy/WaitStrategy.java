package cn.com.sparkle.firefly.net.client.syncwaitstrategy;

import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public interface WaitStrategy {
	public void fireStrategy(PaxosSession session) throws InterruptedException;

	public void finishMessageSend();
}
