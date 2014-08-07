package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class IoSession {
	private final static Logger logger = Logger.getLogger(IoSession.class);

	private long lastActiveTime;
	private SocketChannel channel;
	private MaximumSizeArrayCycleQueue<ByteBuffer> waitSendQueue = new MaximumSizeArrayCycleQueue<ByteBuffer>(ByteBuffer.class, 100);
	private MaximumSizeArrayCycleQueue<IoBuffer> waitSendQueueList = new MaximumSizeArrayCycleQueue<IoBuffer>(IoBuffer.class, 100);
	private NioSocketProcessor processor;
	private IoHandler handler;
	private Object attachment;
	private Entity<IoSession> lastAccessTimeLinkedListwrapSession = null;

	private volatile boolean isSuspendRead = false;

	protected boolean isRegisterReWrite = false;

	private volatile boolean isClose = false;

	private AtomicInteger isGetLast = new AtomicInteger(0);

	private AtomicInteger registerBarrier = new AtomicInteger(0);

	private AtomicBoolean isRegisterClose = new AtomicBoolean(false);

	public IoSession(final NioSocketProcessor processor, final SocketChannel channel, IoHandler handler) {
		this.processor = processor;
		this.channel = channel;
		this.handler = handler;
		this.lastAccessTimeLinkedListwrapSession = new Entity<IoSession>(this);

	}

	public MaximumSizeArrayCycleQueue<ByteBuffer> getDebugQueue() {
		return waitSendQueue;
	}

	public IoHandler getHandler() {
		return handler;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public boolean isClose() {
		return isClose;
	}

	public NioSocketProcessor getProcessor() {
		return processor;
	}

	public boolean isSuspendRead() {
		return isSuspendRead;
	}

	public void suspendRead() {
		//		logger.debug(this.getRemoteAddress() + " suspendRead");
		if (!isSuspendRead) {
			//			logger.debug(this.getRemoteAddress() + " suspendRead");
			isSuspendRead = true;
			processor.unRegisterRead(this);
		}
	}

	public void continueRead() {
		//		logger.debug(this.getRemoteAddress() + " continueRead");
		if (isSuspendRead) {
			//			logger.debug(this.getRemoteAddress() + " continueRead");
			isSuspendRead = false;
			processor.registerRead(this);
		}
	}

	protected AtomicInteger getRegisterBarrier() {
		return registerBarrier;
	}

	public boolean tryWrite(IoBuffer message) throws SessionHavaClosedException {
		return tryWrite(message, true);
	}

	public boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException {
		// this progress of lock is necessary,because the method tryWrite will
		// be invoked in many different threads

		if (isClose) {
			throw new SessionHavaClosedException("IoSession have closed!");
		}
		if (!waitSendQueueList.hasRemain()) {
			return false;
		}

		ByteBuffer buffer = message.getByteBuffer().asReadOnlyBuffer();
		buffer.limit(buffer.position()).position(0);
		if (flush && waitSendQueueList.size() == 0) {
			//try send right now
			try {
				channel.write(buffer);
			} catch (IOException e) {
				this.closeSession();
				throw new SessionHavaClosedException("IoSession have closed!");
			}
			//			if(!buffer.hasRemaining()){
			//				handler.onMessageSent(this, message);
			//				return true;
			//			}
		}
		int flag = registerBarrier.getAndSet(2);

		try {
			waitSendQueueList.push(message);
			waitSendQueue.push(buffer);
		} catch (QueueFullException e) {
			throw new RuntimeException("fatal error", e);
		}
		if (flag == 0) {
			processor.registerWrite(this);
		}
		return true;
	}

	public void write(IoBuffer message) throws SessionHavaClosedException {
		write(message, true);
	}

	public void write(IoBuffer message, boolean flush) throws SessionHavaClosedException {
		while (true) {
			if (tryWrite(message, flush)) {
				break;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
	}

	public IoBuffer getLastWaitSendBuffer() {
		int is = isGetLast.addAndGet(1);
		IoBuffer buffer = waitSendQueueList.last();
		if (is > 0 && buffer != null && buffer.getByteBuffer().hasRemaining()) {
			return buffer;
		} else {
			isGetLast.decrementAndGet();
			return null;
		}
	}

	public void flushLastWaitSendBuffer(IoBuffer buffer) {
		ByteBuffer bb = waitSendQueue.last();
		int flag = registerBarrier.getAndSet(2);

		if (buffer.getByteBuffer().position() > bb.limit()) {
			bb.limit(buffer.getByteBuffer().position());
		}
		isGetLast.decrementAndGet();

		if (flag == 0) {
			// notify NioSocketProcessor to register a write action
			processor.registerWrite(this);
		}
	}

	public MaximumSizeArrayCycleQueue<ByteBuffer>.Bulk peekWaitSendBulk() {
		return waitSendQueue.getBulk();
	}

	public IoBuffer peekIoBuffer() {
		return waitSendQueueList.peek();
	}

	public boolean pollWaitSendBuff() {
		int is = isGetLast.decrementAndGet();
		if (is < 0 && !waitSendQueue.peek().hasRemaining()) {
			truePollWaitSendBuff();
			isGetLast.addAndGet(1);
			return true;
		} else {
			isGetLast.addAndGet(1);
			return false;
		}
	}

	public void truePollWaitSendBuff() {
		waitSendQueue.poll();
		waitSendQueueList.poll();
	}

	public void attach(Object attachment) {
		this.attachment = attachment;
	}

	public Object attachment() {
		return attachment;
	}

	/**
	 * 
	 * @return true if close this connection successfully , false if close unsuccessfully or this connection has been closed.
	 * 
	 */
	public void closeSession() {
		if (!isClose) {
			if (Thread.currentThread() == processor.getThread()) {
				isClose = true;
				isRegisterClose.set(true);
				try {
					channel.close();
					channel.socket().close();
				} catch (IOException e) {
				}
				try {
					handler.onSessionClose(this);
				} catch (Throwable e) {
					logger.error("", e);
				}
			} else {
				//register close request
				if (!isRegisterClose.getAndSet(true)) {
					processor.registerClose(this);
				}
			}
		}
	}

	public String getRemoteAddress() {
		SocketAddress address = null;
		try {
			address = channel.socket().getRemoteSocketAddress();
		} catch (Throwable e) {
			logger.error(e);
		}
		return address == null ? "" : address.toString();
	}

	public String getLocalAddress() {
		SocketAddress address = null;
		try {
			address = channel.socket().getLocalSocketAddress();
		} catch (Throwable e) {
			logger.error(e);
		}
		return address == null ? "" : address.toString();
	}

	public Entity<IoSession> getLastAccessTimeLinkedListwrapSession() {
		lastActiveTime = TimeUtil.currentTimeMillis();
		return lastAccessTimeLinkedListwrapSession;
	}

}
