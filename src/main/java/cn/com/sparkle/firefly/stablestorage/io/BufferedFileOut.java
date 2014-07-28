package cn.com.sparkle.firefly.stablestorage.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.WriteQueue;

public class BufferedFileOut {
	private final static Logger logger = Logger.getLogger(BufferedFileOut.class);

	@SuppressWarnings("unused")
	private volatile static boolean debugLog = true;
	
	private static ArrayBlockingQueue<MyNode> idleBufferPool = new ArrayBlockingQueue<MyNode>(1000);
	
	private final static WriteQueue<BufferedFileOut, BufferPackage, MyNode> waitQueue = new WriteQueue<BufferedFileOut, BufferPackage, MyNode>();
	
	public static WriteQueue<BufferedFileOut, BufferPackage, MyNode> getWaitQueue(){
		return waitQueue;
	}
	public static void init(boolean debugLog,int buffSize,int idleBufferPoolSize) {
		BufferedFileOut.debugLog = debugLog;
		for(int i = 0 ; i < idleBufferPoolSize ; ++i){
			idleBufferPool.add(new MyNode(null, new BufferPackage(buffSize)));
		}
		
		// initial finish event deal thread
		eventThread = new FinishRealEventThread();
		Thread flushEventThread = new Thread(eventThread);
		flushEventThread.setName("Paxos-File-Flush-Event-Thread");
		flushEventThread.start();
		// initial thread to write data into file system truely.
		Thread t = new Thread() {
			public void run() {
				while (true) {
					MyNode n;
					try {
						n = waitQueue.take();
						BufferPackage bp = n.getElement();

						n.getTag().raf.write(bp.buff, 0, bp.used);
						LinkedList<Callable<Object>> calllist = bp.callAbleList;
						bp.callAbleList = new LinkedList<Callable<Object>>();
						n.getTag().finish(n);
						n.getElement().used = 0;
						idleBufferPool.add(n);
						for (Callable<Object> callable : calllist) {
							eventThread.addFinishEvent(callable);
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

	private static class MyNode extends WriteQueue.Node<BufferedFileOut, BufferPackage> {
		public MyNode(BufferedFileOut tag, BufferPackage element) {
			super(tag, element);
		}

		@Override
		public boolean canGet() {
			return this.getElement().used != this.getElement().buff.length;
		}
	}

	private static Callable<Object> nullCallable = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			return null;
		}
	};

//	private ArrayBlockingQueue<MyNode> idleBuffer = new ArrayBlockingQueue<MyNode>(2);
	
	private RandomAccessFile raf;
	private byte[] intbyte = new byte[4];
	private byte[] longbyte = new byte[8];
	private static FinishRealEventThread eventThread;

	private long curPos = 0;

	private ReentrantLock writeLock = new ReentrantLock();
	private ReentrantLock finishLock = new ReentrantLock();
	private Condition finishCondition = finishLock.newCondition();
	private AtomicInteger waitedWriteBuffSize = new AtomicInteger(0);

	// private boolean isClose = false;

	private final static class BufferPackage {
		byte[] buff;
		LinkedList<Callable<Object>> callAbleList = new LinkedList<Callable<Object>>();
		int used = 0;

		public BufferPackage(int size) {
			this.buff = new byte[size];
		}
	}

	public BufferedFileOut(RandomAccessFile randomAccessFile) throws IOException {
		this.raf = randomAccessFile;
		this.curPos = randomAccessFile.getFilePointer();
	}

	/**
	 * 
	 * @param size
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeInt(int size, Callable<Object> callable) throws IOException {
		intbyte[0] = (byte) ((size >>> 24) & 0xFF);
		intbyte[1] = (byte) ((size >>> 16) & 0xFF);
		intbyte[2] = (byte) ((size >>> 8) & 0xFF);
		intbyte[3] = (byte) ((size >>> 0) & 0xFF);
		return write(intbyte, 0, 4, callable);
	}

	/**
	 * 
	 * @param v
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeLong(long v, Callable<Object> callable) throws IOException {
		longbyte[0] = (byte) (0xff & (v >> 56));
		longbyte[1] = (byte) (0xff & (v >> 48));
		longbyte[2] = (byte) (0xff & (v >> 40));
		longbyte[3] = (byte) (0xff & (v >> 32));
		longbyte[4] = (byte) (0xff & (v >> 24));
		longbyte[5] = (byte) (0xff & (v >> 16));
		longbyte[6] = (byte) (0xff & (v >> 8));
		longbyte[7] = (byte) (0xff & v);
		return write(longbyte, 0, 8, callable);
	}

	/**
	 * 
	 * @param buf
	 * @param off
	 * @param length
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long write(byte[] buf, int off, int length, Callable<Object> callable) throws IOException {
		if (callable == null) {
			callable = nullCallable;
		}
		try {
			writeLock.lock();
			long tempCurPos = curPos;

			while (true) {
				MyNode n = waitQueue.getLastNodeOfTag(this);
				if (n == null) {
					try {
						n = idleBufferPool.take();
						waitedWriteBuffSize.incrementAndGet();
						n.setTag(this);
					} catch (InterruptedException e) {
						logger.error("fatal error", e);
						throw new RuntimeException(e);
					}
				}
				BufferPackage bp = n.getElement();
				int canWrite = length > bp.buff.length - bp.used ? bp.buff.length - bp.used : length;
				System.arraycopy(buf, off, bp.buff, bp.used, canWrite);
				off += canWrite;
				length -= canWrite;
				bp.used += canWrite;
				curPos += canWrite;

				if (length == 0) {
					bp.callAbleList.add(callable);
					waitQueue.push(n);
					break;
				} else {
					waitQueue.push(n);
				}
			}
			return tempCurPos;
		} finally {
			writeLock.unlock();
		}
	}

	private void finish(MyNode n) {
		try {
			finishLock.lock();
			
			if(waitedWriteBuffSize.decrementAndGet() == 0){
				finishCondition.signal();
			}
		} finally {
			finishLock.unlock();
		}
	}

	public void close() throws IOException {
		try {
			writeLock.lock();
			if (waitedWriteBuffSize.get() == 0) {
				raf.setLength(raf.getFilePointer());
				raf.close();
				return;
			}

			try {

				finishLock.lock();
				while (waitedWriteBuffSize.get() != 0) {
					try {
						finishCondition.await();
					} catch (InterruptedException e) {
						logger.error("fatal error", e);
						throw new RuntimeException(e);
					}
				}
				raf.setLength(raf.getFilePointer());
				raf.close();
			} finally {
				finishLock.unlock();
			}

		} finally {
			writeLock.unlock();
		}
	}
}
