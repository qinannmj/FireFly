package cn.com.sparkle.firefly.net.netlayer.jvmpipe;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.future.SystemFuture;
import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;

public class JvmPipeClient implements NetClient {
	private final static Logger logger = Logger.getLogger(JvmPipeClient.class);
	
	private final static AtomicLong atomicLong = new AtomicLong(0);
	private NetClient netClient;
	
	private boolean isDebug;
	
	
	

	public JvmPipeClient(NetClient netClient,boolean isDebug) {
		this.netClient = netClient;
		this.isDebug = isDebug;
	}

	@Override
	public Future<Boolean> connect(String ip, int port, Object connectAttachment) throws Throwable {
		String address = ip + port;
		NetHandler serverHandler = JvmPipeServer.addressMap.get(address);
		if (serverHandler != null) {
			SystemFuture<Boolean> future = new SystemFuture<Boolean>();
			future.set(true);
			long flag = atomicLong.addAndGet(1);
			JvmPipePaxosSession clientSession = new JvmPipePaxosSession(netClient.getHandler(), "client-" + flag);
			JvmPipePaxosSession serverSession = new JvmPipePaxosSession(serverHandler, "server-" + flag);
			clientSession.setPeer(serverSession);
			serverSession.setPeer(clientSession);
			netClient.getHandler().onConnect(clientSession, connectAttachment);
			serverHandler.onConnect(serverSession, null);
			if(isDebug){
				logger.debug("build a injvm pipe,id:" + flag);
			}
			return future;
		} else {
			return netClient.connect(ip, port, connectAttachment);
		}
	}

	@Override
	public void init(String path, int heartBeatInterval, NetHandler netHandler) throws Throwable {
		netClient.init(path, heartBeatInterval, netHandler);
	}

	@Override
	public NetHandler getHandler() {
		return this.netClient.getHandler();
	}
}
