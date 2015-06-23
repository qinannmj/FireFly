package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.WriteQueue;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class FlushThreadGroup {
	private final static Logger logger = Logger.getLogger(FlushThreadGroup.class);

	private FinishRealEventThread finishRealEventThread;

	private final WriteQueue<BufferedFileOut, BufferPackage, MyNode> waitQueue = new WriteQueue<BufferedFileOut, BufferPackage, MyNode>();

	private ArrayBlockingQueue<MyNode> idleBufferPool = new ArrayBlockingQueue<MyNode>(1000);

	private LinkedBlockingQueue<BufferedFileOut> allOpenedOut = new LinkedBlockingQueue<BufferedFileOut>();

	public FlushThreadGroup(int buffSize, int idleBufferPoolSize, String groupName) {
		for (int i = 0; i < idleBufferPoolSize; ++i) {
			idleBufferPool.add(new MyNode(null, new WriteBufferPackage(buffSize)));
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
						n = waitQueue.take(2000);
						if (n != null) {
							long ct = TimeUtil.currentTimeMillis();
							BufferPackage bp = n.getElement();
							BufferedFileOut out = n.getTag();
							long pos = n.getTag().getRaf().getFilePointer();
							if (bp.getBuffer() == null) {
								//skip
								RandomAccessFile raf = n.getTag().getRaf();
								raf.seek(raf.getFilePointer() + bp.size());
								if (logger.isDebugEnabled()) {
									logger.debug(String.format("skip to disk cost:%s size:%s pos:%s from:%s bufferinstance:%s", TimeUtil.currentTimeMillis()
											- ct, bp.size(), pos, n.getTag().getRaf().getFD().toString(), bp));
								}
							} else {
								//write
								n.getTag().getRaf().write(bp.getBuffer(), 0, bp.size());
								if (logger.isDebugEnabled()) {
									logger.debug(String.format("flush to disk cost:%s size:%s pos:%s from:%s bufferinstance:%s", TimeUtil.currentTimeMillis()
											- ct, bp.size(), pos, n.getTag().getRaf().getFD().toString(), bp));
								}
								List<Callable<Object>> calllist = bp.getCallableList();
								bp.clear();
								idleBufferPool.add(n);
								finishRealEventThread.addFinishEvent(calllist);
							}
							out.finish();
						} else {
							//flush bufferout
							Iterator<BufferedFileOut> iter = allOpenedOut.iterator();
							while (iter.hasNext()) {
								BufferedFileOut out = iter.next();
								if (!out.isClose()) {
									out.flush();
								} else {
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

	public void open(BufferedFileOut out) {
		allOpenedOut.add(out);
	}

	public WriteQueue<BufferedFileOut, BufferPackage, MyNode> getWaitQueue() {
		return waitQueue;
	}

	public ArrayBlockingQueue<MyNode> getIdleBufferPool() {
		return idleBufferPool;
	}

	public static class MyNode extends WriteQueue.Node<BufferedFileOut, BufferPackage> {
		public MyNode(BufferedFileOut tag, BufferPackage element) {
			super(tag, element);
		}

		@Override
		public boolean canGet() {
			return !this.getElement().isFull();
		}
	}

}
