package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public interface IoSession {

//	public MaximumSizeArrayCycleQueue<ByteBuffer> getDebugQueue();

	public IoHandler getHandler();

	public long getLastActiveTime();

	public SocketChannel getChannel();

	public boolean isClose();

	public NioReadProcessor getReadProcessor();

	public NioWriteProcessor getWriteProcessor();

	public boolean isSuspendRead();

	public void suspendRead();

	public void continueRead();

	public AtomicInteger getRegisterBarrier();

	public boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException;

	public void write(Object msg, boolean tryFlushRightNow) throws SessionHavaClosedException;

	public IoBuffer getLastWaitSendBuffer();

	public void flushLastWaitSendBuffer(IoBuffer buffer);

	public void unlockPeekBuffer();
	
	public IoBuffer lockPeekBuffer();
	
	public void pollWaitSendBuff();

	public void attach(Object attachment);

	public Object attachment();

	/**
	 * 
	 * @return true if close this connection successfully , false if close unsuccessfully or this connection has been closed.
	 * 
	 */
	public void closeSession();

	public String getRemoteAddress();

	public String getLocalAddress();

	public Entity<IoSession> getLastAccessTimeLinkedListwrapSession();

	public boolean isTrafficJam();

	public void setisTrafficJam(boolean on);

}
