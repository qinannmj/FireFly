package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public abstract class AbstractNioProcessor implements NioProcessor {
	protected Selector selector;
	protected Thread thread;
	
	public AbstractNioProcessor(Selector selector){
		this.selector = selector;
	}
	
	public void startProcessor(String name){
		thread = new Thread(new Processor());
		thread.setDaemon(true);
		thread.setName("Raptor-Nio-Processor " + name);
		thread.start();
	}
	
	
	private class Processor implements Runnable {
		public void run() {
			IoSession session;
			while (true) {
				int i;
				try {
					i = selector.select(500);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				if (i > 0) {

					Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						session = (IoSession) key.attachment();
						iter.remove();
						processKey(key, session);
					}
				}
				processAfterSelect();

			}
		}
	}
}
