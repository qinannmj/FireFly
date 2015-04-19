package cn.com.sparkle.firefly.net.netlayer.raptor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.NetServer;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;

public class RaptorServer implements NetServer {
	private final static AtomicInteger INSTANCE_NUM = new AtomicInteger(0);
	private NioSocketServer server;
	private IoHandler handler;
	private String ip = null;
	private int port;

	@Override
	public void init(String confPath, int heartBeatInterval, NetHandler netHandler,String ip,int port, String threadName) throws IOException {
		this.ip = ip;
		this.port = port;
		Conf conf = new Conf(confPath);
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(conf.getIothreadnum());
		nsc.setTcpNoDelay(true);
		nsc.setClearTimeoutSessionInterval(2 * heartBeatInterval);
		nsc.setCycleRecieveBuffCellSize(conf.getCycleRecieveCell());
		nsc.setRecieveBuffSize(conf.getRecieveBuffSize());
		nsc.setSentBuffSize(conf.getSendBuffSize());
		nsc.setBackLog(conf.getBacklog());
		server = new NioSocketServer(nsc, threadName);

		String workthreadName = threadName + INSTANCE_NUM.incrementAndGet();

		if (conf.getWorkthreadMaxNum() == 0) {
			handler = new CodecHandler(conf.getCycleSendCell(), conf.getCycleSendBuffSize(), new BufProtocol(), new RaptorHandler(netHandler));
		} else {
			handler = new MultiThreadHandler(conf.getWorkthreadMinNum(), conf.getWorkthreadMaxNum(), 60, TimeUnit.SECONDS, new CodecHandler(
					conf.getCycleSendCell(), conf.getCycleSendBuffSize(), new BufProtocol(), new RaptorHandler(netHandler)), workthreadName);
		}
	}

	@Override
	public void listen() throws UnknownHostException, IOException {
		if(ip == null) {
			throw new RuntimeException("Not initialize the server!Please run init method!");
		}
		server.waitToBind(new InetSocketAddress(InetAddress.getByName(ip), port), handler);
	}

}
