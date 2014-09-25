package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.nio.channels.SocketChannel;

public interface NioWriteProcessor extends NioProcessor{
	public void registerWrite(IoSession session);
	public void proccedWrite(SocketChannel channel, IoSession session);
}
