package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.AllocateBytesBuff;
import cn.com.sparkle.raptor.core.buff.CycleAllocateBytesBuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.SyncBuffPool;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.Queue;
import cn.com.sparkle.raptor.core.delaycheck.DelayChecked;
import cn.com.sparkle.raptor.core.delaycheck.DelayCheckedTimer;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class NioSocketProcessor {
	private Logger logger = Logger.getLogger(NioSocketProcessor.class);

	// public volatile boolean debug = false;

	private Selector selector;
	private ReentrantLock lock = new ReentrantLock();
	private ReentrantLock readLock = new ReentrantLock();
	private ReentrantLock closeLock = new ReentrantLock();

	private Queue<IoSession> registerQueueWrite = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 1000);
	private Queue<IoSession> registerQueueClose = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 1000);
	private Queue<ReadInterest> registerQueueRead = new MaximumSizeArrayCycleQueue<ReadInterest>(ReadInterest.class, 1000);
	private Queue<IoSession> reRegisterQueueWrite = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 10000);
	private CycleAllocateBytesBuffPool memPool;

	private long lastWarnMemLack = TimeUtil.currentTimeMillis();

	private final static class ReadInterest {
		private IoSession session;
		private boolean isInterest;

		public ReadInterest(IoSession session, boolean isInterest) {
			super();
			this.session = session;
			this.isInterest = isInterest;
		}

	}

	private Thread thread;
	// private RecieveMessageDealer recieveMessageDealer;

	private LastAccessTimeLinkedList<IoSession> activeSessionLinedLinkedList = new LastAccessTimeLinkedList<IoSession>();

	private NioSocketConfigure nscfg;

	private DelayChecked checkRegisterRead;
	private DelayChecked checkRegisterWrite;
	private DelayChecked checkReRegisterWrite;
	private DelayChecked checkTimeoutSession;

	public NioSocketProcessor(NioSocketConfigure nscfg, SyncBuffPool memPool, String name) throws IOException {

		this.nscfg = nscfg;
		selector = Selector.open();
		this.memPool = memPool;

		thread = new Thread(new Processor());
		thread.setDaemon(true);
		thread.setName("Raptor-Nio-Processor " + name);
		thread.start();
		checkRegisterRead = new DelayChecked(nscfg.getRegisterReadDelay()) {
			@Override
			public void goToRun() {
				selector.wakeup();
			}
		};
		checkRegisterWrite = new DelayChecked(nscfg.getRegisterWriteDelay()) {

			@Override
			public void goToRun() {
				selector.wakeup();
			}
		};
		checkReRegisterWrite = new DelayChecked(nscfg.getReRegisterWriteDelay()) {
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
		DelayCheckedTimer.addDelayCheck(checkRegisterWrite);
		DelayCheckedTimer.addDelayCheck(checkReRegisterWrite);
		DelayCheckedTimer.addDelayCheck(checkTimeoutSession);

		checkTimeoutSession.needRun();
	}

	public void unRegisterRead(IoSession session) {
		try {
			readLock.lock();
			while (true) {
				try {
					registerQueueRead.push(new ReadInterest(session, false));
					checkRegisterRead.needRun();
					break;
				} catch (Exception e) {
					logger.debug(e);
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

	public void registerRead(IoSession session) {
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

	public void registerWrite(IoSession session) {
		try {
			lock.lock();
			while (true) {
				try {
					registerQueueWrite.push(session);
					checkRegisterWrite.needRun();
					break;
				} catch (Exception e) {
					logger.debug(e);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {
					}
				}
			}
		} finally {
			lock.unlock();
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
					logger.debug(e);
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

	private boolean changeInterestWrite(SelectionKey key, boolean isInterest, IoSession session) {
		int i = key.interestOps();
		if (isInterest) {
			if ((i & SelectionKey.OP_WRITE) == 0) {
				key.interestOps(i | SelectionKey.OP_WRITE);
				//				logger.debug("write interest true");
				return true;
			}
			return false;
		} else {

			if ((i & SelectionKey.OP_WRITE) != 0) {
				key.interestOps(i ^ SelectionKey.OP_WRITE);
				//				logger.debug("write interest false");
				return true;
			} else
				return false;
		}
	}

	private boolean changeInterestRead(SelectionKey key, boolean isInterest, IoSession session) {
		int i = key.interestOps();
		// logger.debug( "local:" + session.getLocalAddress() + "  remote:" +
		// session.getRemoteAddress() + " interest read:" + isInterest);
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

	private void proceedRegisterWrite() {
		IoSession session;
		while ((session = (IoSession) registerQueueWrite.peek()) != null) {
			registerQueueWrite.poll();
			if (session.peekWaitSendBulk() == null) {
				continue;// if peek() return null indicates the
							// message have proceeded in last send
							// process.
			}
			try {
				SelectionKey key = session.getChannel().keyFor(selector);
				if (key == null) {
					if (session.isClose()) {
						continue;
					}
					key = session.getChannel().register(selector, SelectionKey.OP_WRITE);
					key.attach(session);
				} else {
					key.attach(session);
					changeInterestWrite(key, true, session);
				}
			} catch (Throwable e) {
				activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
				session.getHandler().catchException(session, e);
				session.closeSession();
			}

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

	private void proceedReRegisterWrite() {
		IoSession session;
		// 检查reRegisterQueueWrite是否有已经可以激活的发送session如果有则注册写事件
		long now = TimeUtil.currentTimeMillis();
		while ((session = reRegisterQueueWrite.peek()) != null) {
			if (now - session.getLastActiveTime() < nscfg.getReRegisterWriteDelay()) {
				checkReRegisterWrite.needRun();
				break;
			}
			try {
				if (!session.isClose()) {
					SelectionKey key = session.getChannel().keyFor(selector);
					key.attach(session);
					changeInterestWrite(key, true, session);
					session.isRegisterReWrite = false;
					// logger.debug("change to rewrite" +
					// session.getLastActiveTime());
					// activeSessionLinedLinkedList.putOrMoveFirst(session
					// .getLastAccessTimeLinkedListwrapSession());
				}
			} catch (Throwable e) {
				activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
				session.getHandler().catchException(session, e);
				session.closeSession();
			}

			reRegisterQueueWrite.poll();
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
			logger.debug(entity.getElement() + "  ---" + entity.getElement().getLastActiveTime());
			logger.debug("close timeout connection!" + (now - entity.getElement().getLastActiveTime()));
			activeSessionLinedLinkedList.remove(entity);
		}
	}

	private void proccedRead(SelectionKey key, IoSession session) throws IOException {
		IoBuffer buff = null;
		int readSize = 0;
		try {
			SocketChannel sc = (SocketChannel) key.channel();
			session = (IoSession) key.attachment();
			for (int k = 0; k < 255; ++k) {
				if (buff == null) {
					for (int j = 0; j < 3; j++) {
						if ((buff = memPool.tryGet()) != null) {
							break;
						} else if (TimeUtil.currentTimeMillis() - lastWarnMemLack > 1000) {
							lastWarnMemLack = TimeUtil.currentTimeMillis();
							buff = new AllocateBytesBuff(memPool.getCellCapacity(), false);
							logger.warn("Recieve mem pool is empty!Creat a heap buff!May be you need to increase size of the pool!");
							break;
						}
					}
					if (buff == null) {
						break;
					}
				}
				readSize = sc.read(buff.getByteBuffer());
				if (readSize <= 0) {
					break;
				}
				
				if (!buff.getByteBuffer().hasRemaining()) {
					buff.getByteBuffer().limit(buff.getByteBuffer().position()).position(0);
					session.getHandler().onMessageRecieved(session, buff);
					buff = null;
				}
			}

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

	private void proccedWrite(SelectionKey key, IoSession session) throws IOException {

		boolean isClearWrite = false;
		MaximumSizeArrayCycleQueue<ByteBuffer>.Bulk buffW = session.peekWaitSendBulk();
		session.getRegisterBarrier().set(1);
		if (buffW != null) {
			// set up memory barrier
			SocketChannel sc = (SocketChannel) key.channel();
			long sendSize = 0;
			for (int j = 0; j < nscfg.getTrySendNum(); j++) {
				try {
					sendSize = sc.write(buffW.getQueue(), buffW.getOffset(), buffW.getLength());
				} catch (IllegalArgumentException e) {
					logger.debug(buffW.getOffset() + "  " + buffW.getLength() + "  ");
					throw e;
				}
				if (sendSize != 0)
					break;
			}
			isClearWrite = false;
			for (int j = buffW.getOffset(); j < buffW.getOffset() + buffW.getLength(); j++) {
				if (!buffW.getQueue()[j].hasRemaining()) {
					IoBuffer buffer = session.peekIoBuffer();
					if (session.pollWaitSendBuff()) {
						session.getHandler().onMessageSent(session, buffer);
					} else {
						// isClearWrite = true;
						break;
					}
					sendSize = 1;// avoid session be
					// pushed into
					// reRegisterQueueWrite
				} else {
					break;
				}
			}
			if (session.peekWaitSendBulk() == null) {
				isClearWrite = true;
			} else if (sendSize == 0) {
				// delay send ,and unregister writer;
				try {

					if (!session.isRegisterReWrite) {
						reRegisterQueueWrite.push(session);
						session.isRegisterReWrite = true;
						activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
						checkReRegisterWrite.needRun();
					}
					isClearWrite = true;
					// logger.debug("wait to send");
				} catch (Exception e) {
				}

			}

		} else {
			isClearWrite = true;
		}

		if (isClearWrite) {
			if (session.getRegisterBarrier().getAndSet(0) == 1) {
				changeInterestWrite(key, false, session);
			} else {
				session.getRegisterBarrier().set(1);
			}
		}
	}

	class Processor implements Runnable {

		public void run() {
			IoSession session;
			long lastRunTime = TimeUtil.currentTimeMillis();
			while (true) {
				int i;
				try {
					i = selector.select(1);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				if (TimeUtil.currentTimeMillis() - lastRunTime > 100) {
					logger.debug("slow select cost:" + (TimeUtil.currentTimeMillis() - lastRunTime));
				}
				lastRunTime = TimeUtil.currentTimeMillis();
				if (i > 0) {

					Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					//					logger.debug( "rc " + readCount + "wc " + writeCount +" " +selector.selectedKeys().size());
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						session = (IoSession) key.attachment();
						iter.remove();
						activeSessionLinedLinkedList.putOrMoveFirst(session.getLastAccessTimeLinkedListwrapSession());
						//						logger.debug("active + readable:" + key.isReadable() + " writeable:" + key.isWritable() );
						try {
							if (key.isReadable()) {
								proccedRead(key, session);
								//								++readCount;
							}
							if (key.isWritable()) {
								// logger.debug("send message");
								proccedWrite(key, session);
							}

						} catch (Exception e) {
							session.getHandler().catchException(session, e);
							key.cancel();
							session.closeSession();
							activeSessionLinedLinkedList.remove(session.getLastAccessTimeLinkedListwrapSession());
						}
					}
				}
				proceedRegister();
				proceedRegisterWrite();
				proceedRegisterRead();
				proceedReRegisterWrite();
				proceedTimeoutQueue();

			}
		}
	}
}
