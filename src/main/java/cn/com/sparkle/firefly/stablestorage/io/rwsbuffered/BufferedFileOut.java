package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup.BufferPackage;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup.MyNode;

public class BufferedFileOut implements RecordFileOut{
	private final static Logger logger = Logger.getLogger(BufferedFileOut.class);

	private RandomAccessFile raf;
	private byte[] intbyte = new byte[4];
	private byte[] longbyte = new byte[8];

	private long curPos = 0;

	private ReentrantLock writeLock = new ReentrantLock();
	private ReentrantLock finishLock = new ReentrantLock();
	private Condition finishCondition = finishLock.newCondition();
	private AtomicInteger waitedWriteBuffSize = new AtomicInteger(0);
	private FlushThreadGroup flushThreadGroup;
	// private boolean isClose = false;

	

	public BufferedFileOut(RandomAccessFile randomAccessFile,FlushThreadGroup flushThreadGroup) throws IOException {
		this.raf = randomAccessFile;
		this.curPos = randomAccessFile.getFilePointer();
		this.flushThreadGroup = flushThreadGroup;
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
				MyNode n = flushThreadGroup.getWaitQueue().getLastNodeOfTag(this);
				if (n == null) {
					try {
						n =flushThreadGroup.getIdleBufferPool().take();
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
					flushThreadGroup.getWaitQueue().push(n);
					break;
				} else {
					flushThreadGroup.getWaitQueue().push(n);
				}
			}
			return tempCurPos;
		} finally {
			writeLock.unlock();
		}
	}

	
	public RandomAccessFile getRaf() {
		return raf;
	}

	public void finish(MyNode n) {
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
	
	private static Callable<Object> nullCallable = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			return null;
		}
	};
}
