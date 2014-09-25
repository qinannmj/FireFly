package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class DefaultIoSession implements IoSession {
	private final static Logger logger = Logger.getLogger(DefaultIoSession.class);

	private long lastActiveTime;
	private SocketChannel channel;
	//	private MaximumSizeArrayCycleQueue<ByteBuffer> waitSendQueue = new MaximumSizeArrayCycleQueue<ByteBuffer>(ByteBuffer.class, 1024);
	private MaximumSizeArrayCycleQueue<IoBuffer> waitSendQueueList = new MaximumSizeArrayCycleQueue<IoBuffer>(IoBuffer.class, 1024);
	//	private NioSocketProcessor processor;
	private NioReadProcessor readProcessor;
	private NioWriteProcessor writeProcessor;
	private IoHandler handler;
	private Object attachment;
	private Entity<IoSession> lastAccessTimeLinkedListwrapSession = null;

	private volatile boolean isSuspendRead = false;

	protected boolean isRegisterReWrite = false;

	private volatile boolean isClose = false;

	//	private AtomicInteger isGetLast = new AtomicInteger(0);

	private AtomicReference<IoBuffer> buffLock = new AtomicReference<IoBuffer>();

	private AtomicInteger registerBarrier = new AtomicInteger(0);

	private AtomicBoolean isRegisterClose = new AtomicBoolean(false);

	public DefaultIoSession(final NioReadProcessor readProcessor, final NioWriteProcessor writeProcessor, final SocketChannel channel, IoHandler handler) {
		this.readProcessor = readProcessor;
		this.writeProcessor = writeProcessor;
		this.channel = channel;
		this.handler = handler;
		this.lastAccessTimeLinkedListwrapSession = new Entity<IoSession>(this);

	}

	//	@Override
	//	public MaximumSizeArrayCycleQueue<ByteBuffer> getDebugQueue() {
	//		return waitSendQueue;
	//	}

	@Override
	public IoHandler getHandler() {
		return handler;
	}

	@Override
	public long getLastActiveTime() {
		return lastActiveTime;
	}

	@Override
	public SocketChannel getChannel() {
		return channel;
	}

	@Override
	public boolean isClose() {
		return isClose;
	}

	@Override
	public boolean isSuspendRead() {
		return isSuspendRead;
	}

	@Override
	public void suspendRead() {
		//		logger.debug(this.getRemoteAddress() + " suspendRead");
		if (!isSuspendRead) {
			//			logger.debug(this.getRemoteAddress() + " suspendRead");
			isSuspendRead = true;
			readProcessor.unRegisterRead(this);
		}
	}

	@Override
	public void continueRead() {
		//		logger.debug(this.getRemoteAddress() + " continueRead");
		if (isSuspendRead) {
			//			logger.debug(this.getRemoteAddress() + " continueRead");
			isSuspendRead = false;
			readProcessor.registerRead(this);
		}
	}

	@Override
	public AtomicInteger getRegisterBarrier() {
		return registerBarrier;
	}

	@Override
	public synchronized boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException {
		// this progress of lock is necessary,because the method tryWrite will
		// be invoked in many different threads
		flush = false;
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
		}
		

		try {
			waitSendQueueList.push(message);
			
			//			waitSendQueue.push(buffer);
		} catch (QueueFullException e) {
			throw new RuntimeException("fatal error", e);
		}
		
		int flag = registerBarrier.getAndSet(2);
		if (flag == 0) {
			writeProcessor.registerWrite(this);
//			logger.debug("register writer");
		}
		return true;
	}

	@Override
	public void write(Object msg, boolean flush) throws SessionHavaClosedException {
		while (true) {
			if (tryWrite((IoBuffer) msg, flush)) {
				break;
			}
			if (Thread.currentThread() == getWriteProcessor()) {
				getWriteProcessor().proccedWrite(channel, this);
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public IoBuffer getLastWaitSendBuffer() {
		while (true) {
			IoBuffer lockingBuffer = buffLock.get();
			IoBuffer buffer = waitSendQueueList.last();
			if (buffer == null) {
				return null;
			}
			if (lockingBuffer == buffer) {
				//the last buffer may be using
				return null;
			} else {
				if (buffLock.compareAndSet(lockingBuffer, buffer)) {//try lock
					if(waitSendQueueList.last() == buffer){ //double check
//						logger.debug("lock a buff" + buffer + " " + buffer.getByteBuffer());
						return buffer;
					}else{
						
						buffLock.compareAndSet(buffer, null);
					}
				}
			}
		}
	}

	@Override
	public void flushLastWaitSendBuffer(IoBuffer buffer) {
//		logger.debug("flush  last " + buffer + " " + buffer.getByteBuffer());
		buffLock.set(null);
		int flag = registerBarrier.getAndSet(2);
//		logger.debug(flag);
		if (flag == 0) {
			// notify NioSocketProcessor to register a write action
//			logger.debug("register writer");
			writeProcessor.registerWrite(this);
		}
	}
	@Override
	public void unlockPeekBuffer(){
		IoBuffer buffer = waitSendQueueList.peek();
		ByteBuffer bb = buffer.getByteBuffer();
		bb.mark().position(bb.limit()).limit(bb.capacity());
		buffLock.compareAndSet(buffer, null);
		logger.debug("unlock " + buffer);
		
	}
	@Override
	public IoBuffer lockPeekBuffer() {

		while (true) {
			IoBuffer buffer = waitSendQueueList.peek();
			IoBuffer lockingBuffer = buffLock.get();
			if (lockingBuffer == null) {
				if (buffLock.compareAndSet(null, buffer)) {
					if (buffer != null) {
						try{
							buffer.getByteBuffer().limit(buffer.getByteBuffer().position());
							buffer.getByteBuffer().reset();
						}catch(InvalidMarkException e){
							//first peek
							buffer.getByteBuffer().position(0);
						}
//						logger.debug("peek buffer" + buffer.getByteBuffer() + " " + buffer );
					}else{
//						logger.debug("peek null");
					}
					return buffer;
				}
			} else if (lockingBuffer == buffer) {
//				logger.debug("locked buff " + buffer );
				return null;
			} else {
				if (buffer != null) {
					try{
						buffer.getByteBuffer().reset();
					}catch(InvalidMarkException e){
						//first peek
						buffer.getByteBuffer().limit(buffer.getByteBuffer().position());
						buffer.getByteBuffer().position(0);
					}
//					logger.debug("peek buffer" + buffer.getByteBuffer() + " " + lockingBuffer + " " + buffer);
				}else{
//					logger.debug("peek null");
				}
				return buffer;
			}
		}
	}

	@Override
	public void pollWaitSendBuff() {
		IoBuffer buffer = waitSendQueueList.peek();
		waitSendQueueList.poll();
		buffLock.compareAndSet(buffer, null);
//		logger.debug("poll " + buffer);
	}

	@Override
	public void attach(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public Object attachment() {
		return attachment;
	}

	/**
	 * 
	 * @return true if close this connection successfully , false if close unsuccessfully or this connection has been closed.
	 * 
	 */
	@Override
	public void closeSession() {
		if (!isClose) {
			if (Thread.currentThread() == readProcessor.getThread()) {
				isClose = true;
				isRegisterClose.set(true);
				try {
					channel.close();
					channel.socket().close();
				} catch (IOException e) {
				}
				synchronized (this) {
					IoBuffer buffer;
					while ((buffer = lockPeekBuffer()) != null) {
						buffer.close();
						pollWaitSendBuff();
					}
				}
				try {
					handler.onSessionClose(this);
				} catch (Throwable e) {
					logger.error("", e);
				}
			} else {
				//register close request
				if (!isRegisterClose.getAndSet(true)) {
					readProcessor.registerClose(this);
				}
			}
		}
	}

	@Override
	public String getRemoteAddress() {
		SocketAddress address = null;
		try {
			address = channel.socket().getRemoteSocketAddress();
		} catch (Throwable e) {
			logger.error(e);
		}
		return address == null ? "" : address.toString();
	}

	@Override
	public String getLocalAddress() {
		SocketAddress address = null;
		try {
			address = channel.socket().getLocalSocketAddress();
		} catch (Throwable e) {
			logger.error(e);
		}
		return address == null ? "" : address.toString();
	}

	@Override
	public Entity<IoSession> getLastAccessTimeLinkedListwrapSession() {
		lastActiveTime = TimeUtil.currentTimeMillis();
		return lastAccessTimeLinkedListwrapSession;
	}

	@Override
	public boolean isTrafficJam() {
		return isRegisterReWrite;
	}

	@Override
	public void setisTrafficJam(boolean on) {
		isRegisterReWrite = on;
	}

	@Override
	public NioReadProcessor getReadProcessor() {
		return readProcessor;
	}

	@Override
	public NioWriteProcessor getWriteProcessor() {
		return writeProcessor;
	}
}
