package cn.com.sparkle.firefly.net.netlayer;

import java.util.concurrent.Future;

public interface NetClient {
	public Future<Boolean> connect(String ip, int port, Object connectAttachment) throws Throwable;

	public void init(String path, int heartBeatInterval, NetHandler netHandler) throws Throwable;

	NetHandler getHandler();
}
