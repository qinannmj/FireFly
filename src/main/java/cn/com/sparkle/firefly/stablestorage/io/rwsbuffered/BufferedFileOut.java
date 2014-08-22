package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.io.PriorChangeable;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup.BufferPackage;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup.MyNode;

public class BufferedFileOut implements RecordFileOut ,PriorChangeable{
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

	private MyNode noNeedFluseRightNow = null;
	private volatile boolean isClose = false;

	private boolean isHigh = true;

	private boolean canChIsHigh = true;

	public BufferedFileOut(RandomAccessFile randomAccessFile, FlushThreadGroup flushThreadGroup) throws IOException {
		this.raf = randomAccessFile;
		this.curPos = randomAccessFile.getFilePointer();
		this.flushThreadGroup = flushThreadGroup;
		flushThreadGroup.open(this);
	}

	/**
	 * 
	 * @param size
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeInt(int size, Callable<Object> callable, boolean isSync) throws IOException {
		intbyte[0] = (byte) ((size >>> 24) & 0xFF);
		intbyte[1] = (byte) ((size >>> 16) & 0xFF);
		intbyte[2] = (byte) ((size >>> 8) & 0xFF);
		intbyte[3] = (byte) ((size >>> 0) & 0xFF);
		return write(intbyte, 0, 4, callable, isSync);
	}

	/**
	 * 
	 * @param v
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeLong(long v, Callable<Object> callable, boolean isSync) throws IOException {
		longbyte[0] = (byte) (0xff & (v >> 56));
		longbyte[1] = (byte) (0xff & (v >> 48));
		longbyte[2] = (byte) (0xff & (v >> 40));
		longbyte[3] = (byte) (0xff & (v >> 32));
		longbyte[4] = (byte) (0xff & (v >> 24));
		longbyte[5] = (byte) (0xff & (v >> 16));
		longbyte[6] = (byte) (0xff & (v >> 8));
		longbyte[7] = (byte) (0xff & v);
		return write(longbyte, 0, 8, callable, isSync);
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
	public long write(byte[] buf, int off, int length, Callable<Object> callable, boolean isSync) throws IOException {
		try {
			writeLock.lock();
			long tempCurPos = curPos;
			boolean isFlushRightNow = false;

			while (true) {
				if (noNeedFluseRightNow == null) {
					noNeedFluseRightNow = flushThreadGroup.getWaitQueue().getLastNodeOfTag(this);
					isFlushRightNow = true;
				}
				if (noNeedFluseRightNow == null) {
					try {
						noNeedFluseRightNow = flushThreadGroup.getIdleBufferPool().take();
						waitedWriteBuffSize.incrementAndGet();
						noNeedFluseRightNow.setTag(this);
						isFlushRightNow = false;
					} catch (InterruptedException e) {
						logger.error("fatal error", e);
						throw new RuntimeException(e);
					}
				}
				BufferPackage bp = noNeedFluseRightNow.getElement();
				int canWrite = length > bp.buff.length - bp.used ? bp.buff.length - bp.used : length;
				System.arraycopy(buf, off, bp.buff, bp.used, canWrite);
				off += canWrite;
				length -= canWrite;
				bp.used += canWrite;
				curPos += canWrite;

				if (length == 0) {
					if (callable != null) {
						bp.callAbleList.add(callable);
					}
					isFlushRightNow = isFlushRightNow | isSync;
				}
				if (bp.isFull()) {
					canChIsHigh = false;
					flushThreadGroup.getWaitQueue().push(noNeedFluseRightNow, isHigh);
					noNeedFluseRightNow = null;
				}

				if (length == 0) {
					break;
				}
			}
			if (isFlushRightNow && noNeedFluseRightNow != null) {
				canChIsHigh = false;
				flushThreadGroup.getWaitQueue().push(noNeedFluseRightNow, isHigh);
				noNeedFluseRightNow = null;
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

			if (waitedWriteBuffSize.decrementAndGet() == 0) {
				finishCondition.signal();
			}
		} finally {
			finishLock.unlock();
		}
	}

	public void flush() {
		if (writeLock.tryLock()) {
			try {
				if (noNeedFluseRightNow != null) {
					canChIsHigh = false;
					flushThreadGroup.getWaitQueue().push(noNeedFluseRightNow, isHigh);
					noNeedFluseRightNow = null;
				}
			} finally {
				writeLock.unlock();
			}
		}
	}

	public void close() throws IOException {
		try {
			writeLock.lock();
			if (waitedWriteBuffSize.get() == 0) {
				raf.setLength(raf.getFilePointer());
				logger.debug("close");
				raf.close();
				return;
			}

			try {

				finishLock.lock();
				if (noNeedFluseRightNow != null) {
					canChIsHigh = false;
					flushThreadGroup.getWaitQueue().push(noNeedFluseRightNow, isHigh);
				}
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
			isClose = true;
		} finally {
			writeLock.unlock();
		}
	}

	public boolean isClose() {
		return isClose;
	}

	@Override
	public void setIsHighPrior(boolean isHigh) {
		if(canChIsHigh){
			this.isHigh = isHigh;
		}else{
			throw new RuntimeException("This BufferedOut has write some data!Can't change prior");
		}
	}

}
