package cn.com.sparkle.paxos.net.netlayer.jvmpipe;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.future.SystemFuture;
import cn.com.sparkle.paxos.net.netlayer.NetClient;
import cn.com.sparkle.paxos.net.netlayer.NetHandler;

public class JvmPipeClient implements NetClient {
	private final static Logger logger = Logger.getLogger(JvmPipeClient.class);

	private NetClient netClient;

	private static AtomicLong atomicLong = new AtomicLong(0);

	public JvmPipeClient(NetClient netClient) {
		this.netClient = netClient;
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
			logger.info("build a injvm pipe,id:" + flag);
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
