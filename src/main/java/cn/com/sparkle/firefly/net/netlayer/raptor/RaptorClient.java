package cn.com.sparkle.firefly.net.netlayer.raptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;

public class RaptorClient implements NetClient {

	private final static AtomicInteger INSTANCE_NUM = new AtomicInteger(0);
	private IoHandler handler;
	private NioSocketClient client;
	private NetHandler netHandler;

	@Override
	public Future<Boolean> connect(String ip, int port, Object connectAttachment) throws Exception {
		return client.connect(new InetSocketAddress(ip, port), handler, connectAttachment);
	}

	@Override
	public void init(String path, int heartBeatInterval, NetHandler netHandler,String name) throws IOException {

		Conf conf = new Conf(path);
		String threadName = name + INSTANCE_NUM.incrementAndGet();

		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(conf.getIothreadnum());
		nsc.setTcpNoDelay(true);
		nsc.setClearTimeoutSessionInterval(2 * heartBeatInterval);
		nsc.setCycleRecieveBuffCellSize(conf.getCycleRecieveCell());
		nsc.setRecieveBuffSize(conf.getRecieveBuffSize());
		nsc.setSentBuffSize(conf.getSendBuffSize());
		nsc.setBackLog(conf.getBacklog());
		client = new NioSocketClient(nsc, threadName);
		handler = new MultiThreadHandler(conf.getWorkthreadMinNum(),conf.getWorkthreadMaxNum(),60,TimeUnit.SECONDS,new CodecHandler(conf.getCycleSendCell(), conf.getCycleSendBuffSize(), new BufProtocol(), new RaptorHandler(netHandler)),threadName);
		this.netHandler = netHandler;
	}

	@Override
	public NetHandler getHandler() {
		return this.netHandler;
	}

}
