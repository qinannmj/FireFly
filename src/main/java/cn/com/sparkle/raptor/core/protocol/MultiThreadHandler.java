package cn.com.sparkle.raptor.core.protocol;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.ProxyIoSession;

@SuppressWarnings("rawtypes")
public class MultiThreadHandler implements IoHandler {
	private final static Logger logger = Logger.getLogger(MultiThreadHandler.class);

	private final static int MAX_EVENT_QUEUE_SIZE = 100;
	private final static int CONTINUE_READ_THRESHOLD = (int) (MAX_EVENT_QUEUE_SIZE * 0.6);

	private ThreadPoolExecutor threadPool;
	private IoHandler handler;

	private static abstract class Do<T> {
		public T o;

		public abstract void doJob(IoSession session);
	}

	private class OpenJob extends Do<Object> {
		@Override
		public void doJob(IoSession session) {
			handler.onSessionOpened(session);
		}
	}

	private class CloseJob<T> extends Do<T> {
		@Override
		public void doJob(IoSession session) {
			handler.onSessionClose(session);
		}
	}

	private class SentJob extends Do<Integer> {
		public void doJob(IoSession session) {
			handler.onMessageSent(session, this.o);
		}
	}

	private class ExceptionDo extends Do<Throwable> {
		@Override
		public void doJob(IoSession session) {
			handler.catchException(session.attachment() instanceof MultiThreadIoSession ? (MultiThreadIoSession) session.attachment() : session, o);
		}
	};

	public MultiThreadHandler(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, IoHandler handler, final String threadPoolName) {
		threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			private int count = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread newThread = new Thread(r);
				newThread.setName("Raptor-MultiThreadProtecolHandler-Pool-Thread-" + threadPoolName + (++count));
				return newThread;
			}
		});
		this.handler = handler;
	}

	public MultiThreadHandler(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, IoHandler handler) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, handler, "default");
	}

	@Override
	public final void onSessionOpened(IoSession session) {
		MultiThreadIoSession mySession = new MultiThreadIoSession(session);
		mySession.customAttachment = session.attachment();
		session.attach(mySession);
		onSessionOpen(session);
	}

	private void runOrWaitInQueue(Do<? extends Object> jobDo, IoSession session) {

		if (!(session.attachment() instanceof MultiThreadIoSession)) {// if
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
		MultiThreadIoSession attachment = (MultiThreadIoSession) session.attachment();

		// spin to lock
		synchronized (attachment) {
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

		// activate close event
		Do jobDo = new CloseJob();
		runOrWaitInQueue(jobDo, session);
	}

	@Override
	public void onMessageRecieved(IoSession session, final Object message) throws IOException {
		final MultiThreadIoSession attachment = (MultiThreadIoSession) session.attachment();
		Do<IoBuffer> jobDo = new Do<IoBuffer>() {
			@Override
			public void doJob(IoSession session) {
				try {
					handler.onMessageRecieved(attachment, message);
				} catch (Exception e) {
					logger.error("fatal error", e);
					session.closeSession();
				}
			}
		};
		runOrWaitInQueue(jobDo, session);
	}

	@Override
	public void onMessageSent(IoSession session, int sentSize) {
		SentJob jobDo = new SentJob();
		jobDo.o = sentSize;
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
			MultiThreadIoSession mySession = (MultiThreadIoSession) session.attachment();
			while (true) {
				try {
					jobDo.doJob(mySession);
				} catch (Throwable e) {
					session.closeSession();
					logger.error("", e);
				}
				// spin to lock
				synchronized (mySession) {
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
				}
			}
		}
	}

	public static class MultiThreadIoSession extends ProxyIoSession {
		private LinkedList<Do<? extends Object>> jobQueue = new LinkedList<Do<? extends Object>>();
		//		private volatile byte turn;
		//		private byte wantLock1 = 0, wantLock2 = 0;
		private volatile boolean isExecuting = false;
		public Object customAttachment;

		public MultiThreadIoSession(final IoSession session) {
			super(session);
		}

		public LinkedList<Do<? extends Object>> getJob() {
			return jobQueue;
		}

		@Override
		public void attach(Object attachment) {
			this.customAttachment = attachment;
		}

		@Override
		public Object attachment() {
			return this.customAttachment;
		}
	}

}
