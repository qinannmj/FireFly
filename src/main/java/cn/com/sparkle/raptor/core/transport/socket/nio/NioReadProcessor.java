package cn.com.sparkle.raptor.core.transport.socket.nio;


public interface NioReadProcessor extends NioProcessor {
	public void unRegisterRead(IoSession session);
	public void registerRead(IoSession session);
	public void registerClose(IoSession session);
}
