package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import cn.com.sparkle.raptor.core.buff.BuffPool;

public class NioSocketReadWriteProcessor extends AbstractNioProcessor implements NioReadProcessor,NioWriteProcessor{
	
	private NioReadProcessor read;
	
	private NioWriteProcessor write;
	
	public NioSocketReadWriteProcessor(Selector selector,NioSocketConfigure nscfg, BuffPool memPool) throws IOException {
		super(selector);
		read = new NioSocketReadProcessor(selector, nscfg, memPool);
		write = new NioSocketWriteProcessor(selector, nscfg);
	}

	@Override
	public void processKey(SelectionKey key, IoSession session) {
		read.processKey(key, session);
		write.processKey(key, session);
	}

	@Override
	public void processAfterSelect() {
		read.processAfterSelect();
		write.processAfterSelect();
	}

	@Override
	public Thread getThread() {
		return this.thread;
	}

	@Override
	public void registerWrite(IoSession session) {
		write.registerWrite(session);
	}

	@Override
	public void unRegisterRead(IoSession session) {
		read.unRegisterRead(session);
	}

	@Override
	public void registerRead(IoSession session) {
		read.registerRead(session);
	}

	@Override
	public void registerClose(IoSession session) {
		read.registerClose(session);
	}

	@Override
	public void proccedWrite(SocketChannel channel, IoSession session){
		write.proccedWrite(channel, session);
	}

}
