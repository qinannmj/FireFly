package cn.com.sparkle.paxos.net.netlayer.raptor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.paxos.net.netlayer.NetHandler;
import cn.com.sparkle.paxos.net.netlayer.NetServer;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;

public class RaptorServer implements NetServer {
	private final static AtomicInteger INSTANCE_NUM = new AtomicInteger(0);
	private NioSocketServer server;
	private IoHandler handler;

	@Override
	public void init(String confPath, int heartBeatInterval, NetHandler netHandler) throws IOException {
		Conf conf = new Conf(confPath);
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(conf.getIothreadnum());
		nsc.setTcpNoDelay(true);
		nsc.setClearTimeoutSessionInterval(2 * heartBeatInterval);
		nsc.setCycleRecieveBuffCellSize(conf.getRecieveCell());
		nsc.setRecieveBuffSize(conf.getRecieveBuffSize());
		nsc.setBackLog(conf.getBacklog());
		server = new NioSocketServer(nsc, "server");

		String threadName = "server" + INSTANCE_NUM.incrementAndGet();
		handler = new MultiThreadProtecolHandler(conf.getSendCell(), conf.getSendBuffSize(), conf.getWorkthreadMinNum(), conf.getWorkthreadMaxNum(), 60,
				TimeUnit.SECONDS, new BufProtocol(), new RaptorHandler(netHandler), threadName);
	}

	@Override
	public void listen(String ip, int port) throws UnknownHostException, IOException {
		server.waitToBind(new InetSocketAddress(InetAddress.getByName(ip), port), handler);
	}

}
