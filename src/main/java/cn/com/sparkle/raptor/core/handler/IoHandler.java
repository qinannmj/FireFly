package cn.com.sparkle.raptor.core.handler;

import java.io.IOException;

import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;

public interface IoHandler {
	public void onSessionOpened(IoSession session);

	public void onSessionClose(IoSession session);

	public void onMessageRecieved(IoSession session, Object message) throws IOException;

	public void onMessageSent(IoSession session, int sendSize);

	public void catchException(IoSession session, Throwable e);
}
