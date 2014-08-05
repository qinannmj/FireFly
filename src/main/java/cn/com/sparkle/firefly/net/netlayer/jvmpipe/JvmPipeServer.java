package cn.com.sparkle.firefly.net.netlayer.jvmpipe;

import java.util.concurrent.ConcurrentHashMap;

import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.NetServer;

public class JvmPipeServer implements NetServer {
	public final static ConcurrentHashMap<String, NetHandler> addressMap = new ConcurrentHashMap<String, NetHandler>();

	private NetHandler handler = null;

	private NetServer netServer = null;

	public JvmPipeServer(NetServer netServer) {
		this.netServer = netServer;
	}

	@Override
	public void init(String confPath, int heartBeatInterval, NetHandler handler,String threadName) throws Throwable {
		this.handler = handler;
		netServer.init(confPath, heartBeatInterval, handler,threadName);
	}

	@Override
	public void listen(String ip, int port) throws Throwable {
		String address = ip + port;
		addressMap.put(address, handler);
		netServer.listen(ip, port);
	}
}
