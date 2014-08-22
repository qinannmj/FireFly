package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.WriteQueue;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class FlushThreadGroup {
	private final static Logger logger = Logger.getLogger(FlushThreadGroup.class);
	
	private boolean debug;
	
	private FinishRealEventThread finishRealEventThread;

	private final WriteQueue<BufferedFileOut, BufferPackage, MyNode> waitQueue = new WriteQueue<BufferedFileOut, BufferPackage, MyNode>();

	private ArrayBlockingQueue<MyNode> idleBufferPool = new ArrayBlockingQueue<MyNode>(1000);
	
	private LinkedBlockingQueue<BufferedFileOut> allOpenedOut = new LinkedBlockingQueue<BufferedFileOut>();
	
	public FlushThreadGroup(int buffSize, int idleBufferPoolSize, String groupName,final boolean debug) {
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
						n = waitQueue.take(1000);
						if(n != null){
							BufferPackage bp = n.getElement();
							long ct = TimeUtil.currentTimeMillis();
							n.getTag().getRaf().write(bp.buff, 0, bp.used);
							if(debug){
								logger.debug(String.format("flush to disk cost:%s size:%s from:%s",TimeUtil.currentTimeMillis() - ct,bp.used,n.getTag().getRaf().getFD().toString()));
							}
							LinkedList<Callable<Object>> calllist = bp.callAbleList;
							bp.callAbleList = new LinkedList<Callable<Object>>();
							n.getTag().finish(n);
							n.getElement().used = 0;
							idleBufferPool.add(n);
							finishRealEventThread.addFinishEvent(calllist);
						}else{
							//flush bufferout
							Iterator<BufferedFileOut> iter = allOpenedOut.iterator();
							while(iter.hasNext()){
								BufferedFileOut out = iter.next();
								if(!out.isClose()){
									out.flush();
								}else{
									iter.remove();
								}
							}
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
	public void open(BufferedFileOut out){
		allOpenedOut.add(out);
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
		public boolean isFull(){
			return used == buff.length;
		}
	}
}
