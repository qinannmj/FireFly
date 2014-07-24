package cn.com.sparkle.raptor.core.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.BuffPool.PoolEmptyException;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.SyncBuffPool;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketProcessor;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
@SuppressWarnings("rawtypes")
public class MultiThreadProtecolHandler implements IoHandler {
	private final static Logger logger = Logger.getLogger(MultiThreadProtecolHandler.class);

	private final static int MAX_EVENT_QUEUE_SIZE = 100;
	private final static int CONTINUE_READ_THRESHOLD = (int) (MAX_EVENT_QUEUE_SIZE * 0.6);

	private SyncBuffPool buffPool;
	private ThreadPoolExecutor threadPool;
	private Protocol protocol;
	private ProtocolHandler handler;
	private static AtomicInteger poolEmptyCount = new AtomicInteger(0);

	private Protocol nullProtocol = new Protocol() {
		@Override
		public Object decode(ProtocolHandlerIoSession attachment, IoBuffer buff) {
			return buff;
		}

		@Override
		public IoBuffer[] encode(BuffPool Buffpool, Object obj) {
			throw new RuntimeException("not supported method");
		}

		@Override
		public void init(ProtocolHandlerIoSession attachment) {
		}

		@Override
		public IoBuffer[] encode(BuffPool buffpool, Object message, IoBuffer lastWaitSendBuff) {
			throw new RuntimeException("not supported method");
		}

		@Override
		public Object decode(ProtocolHandlerIoSession mySession) throws IOException {
			return null;
		}

	};

	private static abstract class Do<T> {
		public T o;
		public abstract void doJob(IoSession session);
	}

	private class OpenJob extends Do<Object> {
		@Override
		public void doJob(IoSession session) {
			handler.onOneThreadSessionOpen((ProtocolHandlerIoSession) session.attachment());
		}
	}

	private class CloseJob<T> extends Do<T> {
		@Override
		public void doJob(IoSession session) {
			ProtocolHandlerIoSession mySession = (ProtocolHandlerIoSession) session.attachment();
			handler.onOneThreadSessionClose(mySession);
		}
	}

	private class SentJob extends Do<Integer> {
		public void doJob(IoSession session) {
			handler.onOneThreadMessageSent((ProtocolHandlerIoSession) session.attachment(), this.o);
		}
	}

	private class ExceptionDo extends Do<Throwable> {
		@Override
		public void doJob(IoSession session) {
			handler.onOneThreadCatchException(session,
					session.attachment() instanceof ProtocolHandlerIoSession ? (ProtocolHandlerIoSession) session.attachment() : null, o);
		}
	};

	public MultiThreadProtecolHandler(int sendBuffTotalCellSize, int sendBuffCellCapacity, int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, Protocol protocol, ProtocolHandler handler, final String threadPoolName) {
		buffPool = new SyncBuffPool(sendBuffTotalCellSize, sendBuffCellCapacity);
		threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			private int count = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread newThread = new Thread(r);
				newThread.setName("Raptor-MultiThreadProtecolHandler-Pool-Thread-" + threadPoolName + (++count));
				return newThread;
			}
		});
		this.protocol = protocol;
		this.handler = handler;
		if (protocol == null) {
			protocol = nullProtocol;
		}
	}

	public MultiThreadProtecolHandler(int sendBuffTotalCellSize, int sendBuffCellCapacity, int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, Protocol protocol, ProtocolHandler handler) {
		this(sendBuffTotalCellSize, sendBuffCellCapacity, corePoolSize, maximumPoolSize, keepAliveTime, unit, protocol, handler, "default");
	}

	@Override
	public final void onSessionOpened(IoSession session) {

		ProtocolHandlerIoSession mySession = new ProtocolHandlerIoSession(session, protocol == nullProtocol ? null : protocol, buffPool);
		mySession.customAttachment = session.attachment();
		session.attach(mySession);
		protocol.init(mySession);
		onSessionOpen(session);
	}

	private void runOrWaitInQueue(Do<? extends Object> jobDo, IoSession session) {

		if (!(session.attachment() instanceof ProtocolHandlerIoSession)) {// if
																			// connect
																			// refused,the
																			// session.attachment()
																			// is
																			// not
																			// a
																			// instance
																			// of
			try { // ProtecolHandlerAttachment
				jobDo.doJob(session);
			} catch (Throwable e) {
				session.closeSession();
				logger.error("", e);
			}
			return;
		}
		ProtocolHandlerIoSession attachment = (ProtocolHandlerIoSession) session.attachment();

		// spin to lock

		attachment.wantLock1 = 1;
		attachment.turn = 1;
		while (attachment.turn == 1 && attachment.wantLock2 == 1) {
			Thread.yield();//force to give up cpu
		}
		try {
			if (attachment.isExecuting) {
				// add job to queue
				attachment.jobQueue.addLast(jobDo);
				if (attachment.jobQueue.size() == MAX_EVENT_QUEUE_SIZE) {
					session.suspendRead();// because too many read
				}
			} else {
				// add job to threadpool
				attachment.isExecuting = true;
				threadPool.execute(new JobThread(session, jobDo));
			}
		} finally {
			// release lock
			attachment.wantLock1 = 0;
		}

	}
	@SuppressWarnings("unchecked")
	public void onSessionOpen(IoSession session) {

		Do jobDo = new OpenJob();
		runOrWaitInQueue(jobDo, session);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onSessionClose(IoSession session) {

		// return CycleBuffer to BufferPool
		ProtocolHandlerIoSession mySession = (ProtocolHandlerIoSession) session.attachment();

		try {
			if (mySession != null) {
				mySession.writeLock.lock();
			}
			IoBuffer buffer;
			while ((buffer = session.peekIoBuffer()) != null) {
				buffer.close();
				session.truePollWaitSendBuff();

			}
		} finally {
			if (mySession != null) {
				mySession.writeLock.unlock();
			}
		}
		// activate close event
		Do jobDo = new CloseJob();
		runOrWaitInQueue(jobDo, session);
	}

	@Override
	public void onMessageRecieved(IoSession session, final IoBuffer message) throws IOException {
		final ProtocolHandlerIoSession attachment = (ProtocolHandlerIoSession) session.attachment();
		attachment.recivePackageCount = (attachment.recivePackageCount + 1) % Integer.MAX_VALUE;
		final int recivePackageCount = attachment.recivePackageCount;
		Do<IoBuffer> jobDo = new Do<IoBuffer>() {
			@Override
			public void doJob(IoSession session) {
				try {
					Object obj = protocol.decode(attachment, message);
					attachment.targetRecivePackageCount = recivePackageCount;
					while (obj != null) {
						try {
							handler.onOneThreadMessageRecieved(obj, attachment);
							obj = protocol.decode(attachment);
						} catch (Throwable e) {
							logger.error("error", e);
							session.closeSession();
							break;
						}
					}

				} catch (Exception e) {
					logger.error("fatal error", e);
					session.closeSession();
				}
			}
		};
		runOrWaitInQueue(jobDo, session);
	}

	@Override
	public void onMessageSent(IoSession session, IoBuffer message) {
		SentJob jobDo = new SentJob();
		jobDo.o = message.getByteBuffer().limit();
		message.close();
		runOrWaitInQueue(jobDo, session);
	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		ExceptionDo jobDo = new ExceptionDo();
		jobDo.o = e;
		runOrWaitInQueue(jobDo, session);
	}
	
	private static class JobThread implements Runnable {
		private Do jobDo;
		private IoSession session;

		public JobThread(IoSession session, Do jobDo) {
			this.jobDo = jobDo;
			this.session = session;
		}

		public void run() {
			ProtocolHandlerIoSession mySession = (ProtocolHandlerIoSession) session.attachment();
			while (true) {
				try {
					jobDo.doJob(session);
				} catch (Throwable e) {
					session.closeSession();
					logger.error("", e);
				}
				// spin to lock
				mySession.wantLock2 = 1;
				mySession.turn = 2;
				while (mySession.turn == 2 && mySession.wantLock1 == 1) {
					Thread.yield();
					;//force to give up cpu
				}
				try {
					if (!mySession.jobQueue.isEmpty()) {
						if (mySession.jobQueue.size() == CONTINUE_READ_THRESHOLD && mySession.isSuspendRead()) {
							session.continueRead();
						}
						try {
							jobDo = mySession.jobQueue.removeFirst();
						} catch (NoSuchElementException e) {
							System.err.println(mySession.jobQueue.isEmpty());
							System.err.println(mySession.jobQueue.size());
							throw e;
						}

					} else {
						mySession.isExecuting = false;
						break;
					}
				} finally {
					// release lock
					mySession.wantLock2 = 0;
				}
			}
		}
	}

	public static class ProtocolHandlerIoSession extends IoSession {

		private LinkedList<Do<? extends Object>> jobQueue = new LinkedList<Do<? extends Object>>();
		private volatile byte turn;
		private byte wantLock1 = 0, wantLock2 = 0;
		private volatile boolean isExecuting = false;
		//		private final LinkedList<IoBuffer> unFinishedList = new LinkedList<IoBuffer>();
		public Object protocolAttachment;
		public Object customAttachment;
		private volatile int recivePackageCount = 0;
		private int targetRecivePackageCount = 0;

		private ReentrantLock writeLock = new ReentrantLock();

		private IoSession session;
		private Protocol protocol;
		private SyncBuffPool buffPool;

		public ProtocolHandlerIoSession(final IoSession session, Protocol protocol, SyncBuffPool buffPool) {
			super(null, null, null);
			this.session = session;
			this.protocol = protocol;
			this.buffPool = buffPool;

		}

		public LinkedList<Do<? extends Object>> getJob() {
			return jobQueue;
		}

		@Override
		public boolean isSuspendRead() {
			return session.isSuspendRead();
		}

		@Override
		public IoHandler getHandler() {
			return session.getHandler();
		}

		@Override
		public long getLastActiveTime() {
			return session.getLastActiveTime();
		}

		@Override
		public SocketChannel getChannel() {
			return session.getChannel();
		}

		@Override
		public boolean isClose() {
			return session.isClose();
		}

		@Override
		public NioSocketProcessor getProcessor() {
			return session.getProcessor();
		}

		@Override
		public void suspendRead() {
			session.suspendRead();
		}

		@Override
		public void continueRead() {
			session.continueRead();
		}

		@Override
		public boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException {
			try {
				writeLock.lock();
				return session.tryWrite(message, flush);
			} catch (SessionHavaClosedException e) {

				message.close();
				throw e;
			} finally {
				writeLock.unlock();
			}
		}

		@Override
		public void write(IoBuffer message, boolean flush) throws SessionHavaClosedException {

			try {
				writeLock.lock();
				session.write(message, flush);
			} catch (SessionHavaClosedException e) {
				message.close();
				throw e;
			} finally {
				writeLock.unlock();
			}
		}

		@Override
		public void write(IoBuffer message) throws SessionHavaClosedException {

			try {
				writeLock.lock();
				session.write(message, targetRecivePackageCount == recivePackageCount);
			} catch (SessionHavaClosedException e) {
				message.close();
				throw e;
			} finally {
				writeLock.unlock();
			}
		}

		/**
		 * @param obj
		 * @return the size of bytes writed
		 * @throws SessionHavaClosedException
		 */

		public int writeObject(Object obj) throws SessionHavaClosedException {
			while (true) {
				try {
					int pos = 0;
					int totalSize = 0;
					IoBuffer buff = null;
					try {
						writeLock.lock();
						if (protocol == null) {
							throw new RuntimeException("not supported method");
						}

						buff = session.getLastWaitSendBuffer();
						IoBuffer[] buffs = null;
						if (buff != null) {
							pos = buff.getByteBuffer().position();
						}
						try {
							buffs = protocol.encode(buffPool, obj, buff);
						} catch (PoolEmptyException e) {
							if (buff != null) {
								buff.getByteBuffer().position(pos);
							}
							throw e;
						} finally {
							if (buff != null) {
								session.flushLastWaitSendBuffer(buff);
							}
						}
						if (buff != null) {
							totalSize = buff.getByteBuffer().position() - pos;
						}

						for (int i = 0; i < buffs.length; ++i) {
							try {
								totalSize += buffs[i].getByteBuffer().position();
								session.write(buffs[i], targetRecivePackageCount == recivePackageCount);
							} catch (SessionHavaClosedException e) {
								for (; i < buffs.length; ++i) {
									buffs[i].close();
								}
								throw e;
							}
						}
						return totalSize;
					} catch (IOException e) {
						throw e;
					} finally {
						writeLock.unlock();
					}
				} catch (PoolEmptyException e) {

					int count = poolEmptyCount.incrementAndGet();
					if (count == 10000) {
						poolEmptyCount.set(0);
						logger.warn("maybe you need to incread the size of pool!");
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e1) {
					}
				} catch (IOException e) {
					logger.error("fatal error", e);
					throw new RuntimeException(e);
				}
			}

		}

		@Override
		public void attach(Object attachment) {
			session.attach(attachment);
		}

		@Override
		public IoBuffer getLastWaitSendBuffer() {
			return session.getLastWaitSendBuffer();
		}

		@Override
		public void flushLastWaitSendBuffer(IoBuffer buffer) {
			session.flushLastWaitSendBuffer(buffer);
		}

		@Override
		public IoBuffer peekIoBuffer() {
			return session.peekIoBuffer();
		}

		@Override
		public MaximumSizeArrayCycleQueue<ByteBuffer>.Bulk peekWaitSendBulk() {
			return session.peekWaitSendBulk();
		}

		@Override
		public boolean pollWaitSendBuff() {
			return session.pollWaitSendBuff();
		}

		@Override
		public Object attachment() {
			return session.attachment();
		}

		@Override
		public void closeSession() {
			session.closeSession();
		}

		@Override
		public Entity<IoSession> getLastAccessTimeLinkedListwrapSession() {
			return session.getLastAccessTimeLinkedListwrapSession();
		}
	}

}
