package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.nio.channels.SelectionKey;

public interface NioProcessor {
	public void processKey(SelectionKey key,IoSession session);
	public void processAfterSelect();
	public Thread getThread();
	
}
