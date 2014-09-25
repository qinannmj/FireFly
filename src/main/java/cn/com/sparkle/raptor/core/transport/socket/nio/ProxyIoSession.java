package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.collections.LastAccessTimeLinkedList.Entity;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public abstract class ProxyIoSession implements IoSession {
	private IoSession inner;

	public ProxyIoSession(IoSession inner) {
		this.inner = inner;
	}

	public void innerAttach(Object obj) {
		inner.attach(obj);
	}

	public Object innerAttachment() {
		return inner.attachment();
	}


	@Override
	public IoHandler getHandler() {
		return inner.getHandler();
	}

	@Override
	public long getLastActiveTime() {
		return inner.getLastActiveTime();
	}

	@Override
	public SocketChannel getChannel() {
		return inner.getChannel();
	}

	@Override
	public boolean isClose() {
		return inner.isClose();
	}

	@Override
	public NioReadProcessor getReadProcessor() {
		return inner.getReadProcessor();
	}

	@Override
	public NioWriteProcessor getWriteProcessor() {
		return inner.getWriteProcessor();
	}

	@Override
	public boolean isSuspendRead() {
		return inner.isSuspendRead();
	}

	@Override
	public void suspendRead() {
		inner.suspendRead();
	}

	@Override
	public void continueRead() {
		inner.continueRead();
	}

	@Override
	public AtomicInteger getRegisterBarrier() {
		return inner.getRegisterBarrier();
	}

	@Override
	public boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException {
		return inner.tryWrite(message, flush);
	}

	@Override
	public void write(Object msg, boolean tryFlushRightNow) throws SessionHavaClosedException {
		inner.write(msg, tryFlushRightNow);
	}

	@Override
	public IoBuffer getLastWaitSendBuffer() {
		return inner.getLastWaitSendBuffer();
	}

	@Override
	public void flushLastWaitSendBuffer(IoBuffer buffer) {
		inner.flushLastWaitSendBuffer(buffer);
	}
	@Override
	public void unlockPeekBuffer() {
		inner.unlockPeekBuffer();
	}
	@Override
	public IoBuffer lockPeekBuffer() {
		return inner.lockPeekBuffer();
	}

	@Override
	public void pollWaitSendBuff() {
		inner.pollWaitSendBuff();
	}

	@Override
	public void closeSession() {
		inner.closeSession();
	}

	@Override
	public String getRemoteAddress() {
		return inner.getRemoteAddress();
	}

	@Override
	public String getLocalAddress() {
		return inner.getLocalAddress();
	}

	@Override
	public Entity<IoSession> getLastAccessTimeLinkedListwrapSession() {
		return inner.getLastAccessTimeLinkedListwrapSession();
	}

	@Override
	public boolean isTrafficJam() {
		return inner.isTrafficJam();
	}

	@Override
	public void setisTrafficJam(boolean on) {
		inner.setisTrafficJam(on);
	}

}
