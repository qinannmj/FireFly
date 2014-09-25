package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.Queue;
import cn.com.sparkle.raptor.core.delaycheck.DelayChecked;
import cn.com.sparkle.raptor.core.delaycheck.DelayCheckedTimer;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class NioSocketWriteProcessor extends AbstractNioProcessor implements NioWriteProcessor {
	private Logger logger = Logger.getLogger(NioSocketWriteProcessor.class);

	// public volatile boolean debug = false;

	private ReentrantLock lock = new ReentrantLock();

	private Queue<IoSession> registerQueueWrite = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 1000);
	private Queue<IoSession> reRegisterQueueWrite = new MaximumSizeArrayCycleQueue<IoSession>(IoSession.class, 10000);

	private NioSocketConfigure nscfg;

	private DelayChecked checkRegisterWrite;
	private DelayChecked checkReRegisterWrite;

	public NioSocketWriteProcessor(final Selector selector, NioSocketConfigure nscfg) throws IOException {
		super(selector);
		this.nscfg = nscfg;
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

		DelayCheckedTimer.addDelayCheck(checkRegisterWrite);
		DelayCheckedTimer.addDelayCheck(checkReRegisterWrite);
	}

	public void registerWrite(IoSession session) {
		if (Thread.currentThread() == this.thread) {
			changeInterestWrite(session.getChannel().keyFor(selector), true, session);
		} else {
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

	private void proceedRegisterWrite() {
		IoSession session;
		while ((session = (IoSession) registerQueueWrite.peek()) != null) {
			registerQueueWrite.poll();
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
					session.setisTrafficJam(false);
				}
			} catch (Throwable e) {
				session.getHandler().catchException(session, e);
				session.closeSession();
			}

			reRegisterQueueWrite.poll();
		}
	}

	public void proccedWrite(SocketChannel channel, IoSession session) {
		SelectionKey key = channel.keyFor(selector);
		try {
			proccedWrite(key, session);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void proccedWrite(SelectionKey key, IoSession session) throws IOException {
		IoBuffer buff = session.lockPeekBuffer();
		try {
			if (buff != null) {
				// set up memory barrier
				SocketChannel sc = (SocketChannel) key.channel();
				long sendSize = 0;
				for (int j = 0; j < nscfg.getTrySendNum(); j++) {
					try {
						//					sendSize = sc.write(buffW.getQueue(), buffW.getOffset(), buffW.getLength());
//						logger.debug(buff + String.format("pos:%s limit:%s", buff.getByteBuffer().position(),buff.getByteBuffer().limit()));
						sendSize = sc.write(buff.getByteBuffer());
//						logger.debug("write" + sendSize);
					} catch (IllegalArgumentException e) {
						throw e;
					} catch (BufferOverflowException e) {
						throw e;
					}
					if (sendSize == 0)
						break;
				}
				if (!buff.getByteBuffer().hasRemaining()) {
//					logger.debug("poll buffer" + buff.getByteBuffer());
					int size = buff.getByteBuffer().limit();
					session.pollWaitSendBuff();
					buff.close();
					buff = null;
					session.getHandler().onMessageSent(session, size);
					sendSize = 1;// avoid session be pushed into reRegisterQueueWrite
				}else{
					session.unlockPeekBuffer();
				}
			} else {
				int flag = session.getRegisterBarrier().getAndSet(0);
				if (flag == 1) {
					changeInterestWrite(key, false, session);
				} else {
					session.getRegisterBarrier().set(1);
				}
			}
		} catch (IOException e) {
			if (buff != null) {
				session.pollWaitSendBuff();
			}
		}
	}

	@Override
	public void processKey(SelectionKey key, IoSession session) {
		try {
			if (key.isWritable()) {
				proccedWrite(key, session);
			}

		} catch (Exception e) {
			session.getHandler().catchException(session, e);
			key.cancel();
			session.closeSession();
		}

	}

	@Override
	public void processAfterSelect() {
		proceedRegisterWrite();
		proceedReRegisterWrite();
	}

	@Override
	public Thread getThread() {
		// TODO Auto-generated method stub
		return null;
	}
}
