package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.Queue;
import cn.com.sparkle.raptor.core.delaycheck.DelayChecked;
import cn.com.sparkle.raptor.core.delaycheck.DelayCheckedTimer;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class NioSocketReadProcessor extends AbstractNioProcessor implements NioReadProcessor {
	private Logger logger = Logger.getLogger(NioSocketReadProcessor.class);

	private ReentrantLock readLock = new ReentrantLock();
	private ReentrantLock closeLock = new ReentrantLock();
	private Queue<IoSession> registerQueueClose = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 1000);
	private Queue<ReadInterest> registerQueueRead = new MaximumSizeArrayCycleQueue<ReadInterest>(ReadInterest.class, 1000);
	private BuffPool memPool;

	private final static class ReadInterest {
		private IoSession session;
		private boolean isInterest;

		public ReadInterest(IoSession session, boolean isInterest) {
			super();
			this.session = session;
			this.isInterest = isInterest;
		}

	}

	private LastAccessTimeLinkedList<IoSession> activeSessionLinedLinkedList = new LastAccessTimeLinkedList<IoSession>();

	private NioSocketConfigure nscfg;

	private DelayChecked checkRegisterRead;
	private DelayChecked checkTimeoutSession;

	public NioSocketReadProcessor(final Selector selector, NioSocketConfigure nscfg, BuffPool memPool) throws IOException {
		super(selector);
		this.nscfg = nscfg;
		this.memPool = memPool;
		checkRegisterRead = new DelayChecked(nscfg.getRegisterReadDelay()) {
			@Override
			public void goToRun() {
				selector.wakeup();
			}
		};
		checkTimeoutSession = new DelayChecked(nscfg.getClearTimeoutSessionInterval(), true) {
			@Override
			public void goToRun() {
				selector.wakeup();
			}
		};
		DelayCheckedTimer.addDelayCheck(checkRegisterRead);
		DelayCheckedTimer.addDelayCheck(checkTimeoutSession);

		checkTimeoutSession.needRun();
	}

	public void unRegisterRead(IoSession session) {
		if (Thread.currentThread() == this.thread) {
			changeInterestRead(session.getChannel().keyFor(selector), false, session);
		} else {
			try {
				readLock.lock();
				while (true) {
					try {
						registerQueueRead.push(new ReadInterest(session, false));
						checkRegisterRead.needRun();
						break;
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug(e);
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException e1) {
						}
					}
				}
			} finally {
				readLock.unlock();
			}
		}

	}

	public void registerRead(IoSession session) {
		if (Thread.currentThread() == this.thread) {
			changeInterestRead(session.getChannel().keyFor(selector), true, session);
		} else {
			try {
				readLock.lock();
				while (true) {
					try {
						registerQueueRead.push(new ReadInterest(session, true));
						checkRegisterRead.needRun();
						break;
					} catch (Exception e) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e1) {
						}
					}
				}
			} finally {
				readLock.unlock();
			}
		}
	}

	public void registerClose(IoSession session) {
		try {
			closeLock.lock();
			while (true) {
				try {
					registerQueueClose.push(session);
					selector.wakeup();
					break;
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug(e);
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {
					}
				}
			}
		} finally {
			closeLock.unlock();
		}
	}

	private boolean changeInterestRead(SelectionKey key, boolean isInterest, IoSession session) {
		int i = key.interestOps();
		if (isInterest) {
			if ((i & SelectionKey.OP_READ) == 0) {
				key.interestOps(i | SelectionKey.OP_READ);
				return true;
			} else
				return false;
		} else {

			if ((i & SelectionKey.OP_READ) != 0) {
				key.interestOps(i ^ SelectionKey.OP_READ);
				return true;
			} else
				return false;
		}
	}

	public Thread getThread() {
		return thread;
	}

	private void proceedRegister() {
		IoSession session;
		while ((session = (IoSession) registerQueueClose.peek()) != null) {
			registerQueueClose.poll();
			session.closeSession();
		}
	}

	private void proceedRegisterRead() {
		ReadInterest readInterest = null;
		IoSession session;
		while ((readInterest = (ReadInterest) registerQueueRead.peek()) != null) {
			session = readInterest.session;
			registerQueueRead.poll();

			try {
				SelectionKey key = session.getChannel().keyFor(selector);
				if (key == null) {
					if (readInterest.isInterest) {
						key = session.getChannel().register(selector, SelectionKey.OP_READ);
						key.attach(session);
					}
				} else {
					changeInterestRead(key, readInterest.isInterest, session);
				}
			} catch (Throwable e) {
				activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
				session.getHandler().catchException(session, e);
				session.closeSession();
			}

		}
	}

	private void proceedTimeoutQueue() {
		long now = TimeUtil.currentTimeMillis();
		Entity<IoSession> entity;
		while ((entity = activeSessionLinedLinkedList.getLast()) != null) {
			if (now - entity.getElement().getLastActiveTime() < nscfg.getClearTimeoutSessionInterval()) {
				break;
			}
			entity.getElement().closeSession();
			if (logger.isDebugEnabled()) {
				logger.debug("close timeout connection!" + (now - entity.getElement().getLastActiveTime()));
			}
			activeSessionLinedLinkedList.remove(entity);
		}
	}

	private void proccedRead(SelectionKey key, IoSession session) throws IOException {
		IoBuffer buff = null;
		int readSize = 0;
		try {
			SocketChannel sc = (SocketChannel) key.channel();
			session = (IoSession) key.attachment();
			//			int totalRead = 0;
			for (int k = 0; k < 255; ++k) {
				if (buff == null) {
					buff = memPool.get();
				}
				readSize = sc.read(buff.getByteBuffer());
				//				logger.debug("read " + readSize);
				if (readSize <= 0) {
					break;
				}
				//				totalRead += readSize;
				if (!buff.getByteBuffer().hasRemaining()) {
					buff.getByteBuffer().limit(buff.getByteBuffer().position()).position(0);
					session.getHandler().onMessageRecieved(session, buff);
					buff = null;
				}
			}
			//			System.out.println("totalRead:"+totalRead);
			if (buff != null && buff.getByteBuffer().position() != 0) {
				buff.getByteBuffer().limit(buff.getByteBuffer().position()).position(0);
				session.getHandler().onMessageRecieved(session, buff);
			} else if (buff != null) {
				buff.close();
			}
			if (readSize < 0) {
				session.closeSession();
				activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
			}
		} catch (IOException e) {
			if (buff != null) {
				buff.close();
			}
			throw e;
		}
	}

	@Override
	public void processKey(SelectionKey key, IoSession session) {
		activeSessionLinedLinkedList.putOrMoveFirst(session.getLastAccessTimeLinkedListwrapSession());
		try {
			if (key.isReadable()) {
				proccedRead(key, session);
			}
		} catch (Exception e) {
			session.getHandler().catchException(session, e);
			key.cancel();
			session.closeSession();
			activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
		}
	}

	@Override
	public void processAfterSelect() {
		proceedRegister();
		proceedRegisterRead();
		proceedTimeoutQueue();

	}
}
