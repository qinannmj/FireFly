package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.delaycheck.DelayChecked;
import cn.com.sparkle.raptor.core.delaycheck.DelayCheckedTimer;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.factory.ProcessorGroup;

public class NioSocketConnector {
	private Selector selector;
	private MultNioSocketProcessor multNioSocketProcessor;
	NioSocketConfigure nscfg;
	private MaximumSizeArrayCycleQueue<QueueBean> waitConnectQueue = new MaximumSizeArrayCycleQueue<NioSocketConnector.QueueBean>(
			NioSocketConnector.QueueBean.class, 10000);
	private DelayChecked checkRegisterConnecter;

	private Runnable nullRunnable = new Runnable() {
		@Override
		public void run() {
		}
	};

	private class QueueBean {
		SocketChannel sc;
		IoHandler handler;
		Object attachment;
		ConnectFuture future;
		IoSession session;
	}

	public static class ConnectFuture extends FutureTask<Boolean> {
		public ConnectFuture(Runnable runnable, Boolean result) {
			super(runnable, result);
		}

		public void setResult(Boolean v) {
			super.set(v);
		}
	}

	public NioSocketConnector(NioSocketConfigure nscfg, String name) throws IOException {
		this.nscfg = nscfg;
		this.multNioSocketProcessor = new MultNioSocketProcessor(nscfg, name);
		selector = Selector.open();
		Thread t = new Thread(new Connector());
		t.setName("Raptor-Nio-Connector " + name);
		t.setDaemon(nscfg.isDaemon());
		t.start();
		checkRegisterConnecter = new DelayChecked(nscfg.getRegisterConnecterDelay()) {
			@Override
			public void goToRun() {
				selector.wakeup();
			}
		};
		DelayCheckedTimer.addDelayCheck(checkRegisterConnecter);
	}

	public Future<Boolean> registerConnector(SocketChannel sc, IoHandler handler, Object attachment) throws Exception {
		ConnectFuture futureTask = new ConnectFuture(nullRunnable, true);
		QueueBean a = new QueueBean();
		a.handler = handler;
		a.sc = sc;
		a.attachment = attachment;
		a.future = futureTask;
		waitConnectQueue.push(a);
		checkRegisterConnecter.needRun();

		return a.future;
	}

	class Connector implements Runnable {
		public void run() {
			while (true) {
				int i;
				try {
					i = selector.select(1);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				QueueBean qb;
				// long s = System.currentTimeMillis();
				while ((qb = waitConnectQueue.peek()) != null) {
					ProcessorGroup processor = multNioSocketProcessor.getProcessor();
					IoSession session = new DefaultIoSession(processor.getReadProcessor(),processor.getWriteProcessor(), qb.sc, qb.handler);
					qb.session = session;
					session.attach(qb.attachment);
					try {
						qb.sc.register(selector, SelectionKey.OP_CONNECT, qb);
					} catch (ClosedChannelException e) {
						qb.handler.catchException(session, e);
						qb.future.setResult(false);
						qb.future.run();
					}
					waitConnectQueue.poll();
				}

				if (i > 0) {

					Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();
						if (key.isConnectable()) {
							key.cancel();
							SocketChannel sc = (SocketChannel) key.channel();
							QueueBean queueBean = (QueueBean) key.attachment();
							try {
								if (sc.finishConnect()) {
									queueBean.session.getReadProcessor().registerRead(queueBean.session);
									queueBean.session.getHandler().onSessionOpened(queueBean.session);
									queueBean.future.setResult(true);
									queueBean.future.run();
								}
							} catch (Exception e) {
								queueBean.future.setResult(false);
								queueBean.future.run();
								queueBean.session.getHandler().catchException(queueBean.session, e);
								try {
									sc.close();
								} catch (Exception ee) {
								}

							}
						}
					}
				}
				// System.out.println("cost:"+(System.currentTimeMillis() - s));

			}
		}
	}
}
