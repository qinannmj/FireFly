package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.WriteQueue;

public class FlushThreadGroup {
	private final static Logger logger = Logger.getLogger(FlushThreadGroup.class);
	
	private boolean debug;
	
	private FinishRealEventThread finishRealEventThread;

	private final WriteQueue<BufferedFileOut, BufferPackage, MyNode> waitQueue = new WriteQueue<BufferedFileOut, BufferPackage, MyNode>();

	private ArrayBlockingQueue<MyNode> idleBufferPool = new ArrayBlockingQueue<MyNode>(1000);

	public FlushThreadGroup(int buffSize, int idleBufferPoolSize, String groupName,boolean debug) {
		this.debug = debug;
		for (int i = 0; i < idleBufferPoolSize; ++i) {
			idleBufferPool.add(new MyNode(null, new BufferPackage(buffSize)));
		}
		// initial finish event deal thread
		finishRealEventThread = new FinishRealEventThread();
		Thread flushEventThread = new Thread(finishRealEventThread);
		flushEventThread.setName(groupName + "-File-Flush-Event-Thread");
		flushEventThread.start();

		// initial thread to write data into file system truely.
		Thread t = new Thread() {
			public void run() {
				while (true) {
					MyNode n;
					try {
						n = waitQueue.take();
						BufferPackage bp = n.getElement();
						n.getTag().getRaf().write(bp.buff, 0, bp.used);
						LinkedList<Callable<Object>> calllist = bp.callAbleList;
						bp.callAbleList = new LinkedList<Callable<Object>>();
						n.getTag().finish(n);
						n.getElement().used = 0;
						idleBufferPool.add(n);
						for (Callable<Object> callable : calllist) {
							finishRealEventThread.addFinishEvent(callable);
						}
					} catch (Throwable e) {
						logger.error("fatal error", e);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
						}
						System.exit(1);// quickly close process
					}

				}
			}
		};
		t.setName("Paxos-File-Flush-Thread");
		t.start();
	}

	public WriteQueue<BufferedFileOut, BufferPackage, MyNode> getWaitQueue() {
		return waitQueue;
	}
	

	public ArrayBlockingQueue<MyNode> getIdleBufferPool() {
		return idleBufferPool;
	}


	public boolean isDebug() {
		return debug;
	}


	public static class MyNode extends WriteQueue.Node<BufferedFileOut, BufferPackage> {
		public MyNode(BufferedFileOut tag, BufferPackage element) {
			super(tag, element);
		}

		@Override
		public boolean canGet() {
			return this.getElement().used != this.getElement().buff.length;
		}
	}

	public final static class BufferPackage {
		byte[] buff;
		LinkedList<Callable<Object>> callAbleList = new LinkedList<Callable<Object>>();
		int used = 0;

		public BufferPackage(int size) {
			this.buff = new byte[size];
		}
	}
}
