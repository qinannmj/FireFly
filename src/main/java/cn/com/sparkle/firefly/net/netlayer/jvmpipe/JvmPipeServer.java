package cn.com.sparkle.firefly.net.netlayer.jvmpipe;

import java.util.concurrent.ConcurrentHashMap;

import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.NetServer;

public class JvmPipeServer implements NetServer {
	public final static ConcurrentHashMap<String, NetHandler> addressMap = new ConcurrentHashMap<String, NetHandler>();


	private NetServer netServer = null;
	
	
	public JvmPipeServer(NetServer netServer) {
		this.netServer = netServer;
	}

	@Override
	public void init(String confPath, int heartBeatInterval, NetHandler handler,String ip,int port,String threadName) throws Throwable {
		netServer.init(confPath, heartBeatInterval, handler,ip,port,threadName);
		String address = ip + ":" + port;
		addressMap.put(address, handler);
	}

	@Override
	public void listen() throws Throwable {
		netServer.listen();
	}
}
