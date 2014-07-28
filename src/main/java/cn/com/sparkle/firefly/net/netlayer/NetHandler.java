package cn.com.sparkle.firefly.net.netlayer;

import cn.com.sparkle.firefly.net.netlayer.buf.Buf;

public interface NetHandler {
	public void onDisconnect(PaxosSession session);

	public void onConnect(PaxosSession session, Object connectAttachment);

	public void onRecieve(PaxosSession session, Buf buffer) throws InterruptedException;

	public void onRefuse(Object connectAttachment);
}
