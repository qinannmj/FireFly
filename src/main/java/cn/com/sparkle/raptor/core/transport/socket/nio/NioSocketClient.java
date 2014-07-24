package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.raptor.core.handler.IoHandler;

public class NioSocketClient {
	private NioSocketConfigure nscfg;
	private NioSocketConnector connector;
	private Lock lock = new ReentrantLock();

	public NioSocketClient(NioSocketConfigure nscfg, String name) throws IOException {
		this.nscfg = nscfg;
		connector = new NioSocketConnector(nscfg, name);
	}

	public NioSocketClient(NioSocketConfigure nscfg) throws IOException {
		this(nscfg, "defaultClient");
	}

	private SocketChannel getSocketChannel() throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		nscfg.configurateSocket(sc.socket());
		return sc;
	}

	public Future<Boolean> connect(InetSocketAddress address, IoHandler handler) throws Exception {
		return connect(address, handler, null);

	}

	public Future<Boolean> connect(InetSocketAddress address, IoHandler handler, Object attachment) throws Exception {

		if (handler == null)
			throw new IOException("handler is not exist");
		SocketChannel sc = getSocketChannel();
		sc.connect(address);
		try {
			lock.lock();
			return connector.registerConnector(sc, handler, attachment);

		} finally {
			lock.unlock();
		}
	}
}
