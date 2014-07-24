package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.handler.IoHandler;

public class NioSocketAccepter {

	//	private final static Logger logger = Logger.getLogger(NioSocketAccepter.class);
	private Selector selector;
	private NioSocketConfigure nscfg;
	private MultNioSocketProcessor multNioSocketProcessor;
	private MaximumSizeArrayCycleQueue<QueueBean> waitRegisterQueue = new MaximumSizeArrayCycleQueue<NioSocketAccepter.QueueBean>(
			NioSocketAccepter.QueueBean.class, 100);

	private class QueueBean {
		ServerSocketChannel ssc;
		IoHandler handler;
	}

	public NioSocketAccepter(NioSocketConfigure nscfg, String name) throws IOException {
		this.nscfg = nscfg;
		this.multNioSocketProcessor = new MultNioSocketProcessor(nscfg, name);
		selector = Selector.open();
		Thread t = new Thread(new Accepter());
		t.setName("Raptor-Nio-Acceptor " + name);
		t.setDaemon(nscfg.isDaemon());
		t.start();
	}

	public void registerAccept(ServerSocketChannel ssc, IoHandler handler) throws IOException, QueueFullException {
		if (handler == null)
			throw new IOException("handler can't be null");
		QueueBean qb = new QueueBean();
		qb.ssc = ssc;
		qb.handler = handler;
		waitRegisterQueue.push(qb);
		selector.wakeup();
	}

	class Accepter implements Runnable {
		public void run() {
			while (true) {
				try {
					int i = selector.select(1);

					QueueBean qb;
					while ((qb = waitRegisterQueue.peek()) != null) {
						qb.ssc.register(selector, SelectionKey.OP_ACCEPT, qb.handler);
						waitRegisterQueue.poll();
					}
					if (i > 0) {

						Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							iter.remove();
							if (key.isAcceptable()) {

								SocketChannel sc = null;
								try {
									IoHandler handler = (IoHandler) key.attachment();
									sc = ((ServerSocketChannel) key.channel()).accept();
									sc.configureBlocking(false);
									nscfg.configurateSocket(sc.socket());

									NioSocketProcessor processor = multNioSocketProcessor.getProcessor();
									IoSession session = new IoSession(processor, sc, handler);
									handler.onSessionOpened(session);
									processor.registerRead(session);
								} catch (IOException e) {
									if (sc != null) {
										try {
											sc.close();
										} catch (Exception ee) {
										}
									}
								}
							}

						}
					}
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
