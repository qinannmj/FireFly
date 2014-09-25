package cn.com.sparkle.raptor.core.buff;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class SingleSyncBuffPool extends CycleAllocateBytesBuffPool {
	private final static Logger logger = Logger.getLogger(SingleSyncBuffPool.class);
	private String name;
	public SingleSyncBuffPool(int totalCellSize, int cellCapacity,String name) {
		super(totalCellSize, cellCapacity);
		this.name = name;
	}

	private ReentrantLock lock = new ReentrantLock();

	@Override
	public boolean close(CycleBuff buff) {
		lock.lock();
		try {
			return super.close(buff);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public IoBufferArray get(int byteSize) {
		lock.lock();
		try {
			if (byteSize < 1) {
				return new IoBufferArray(new IoBuffer[0]);
			}
			return super.get(byteSize);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public IoBuffer get() {
		lock.lock();
		try {
			return super.get();
		} finally {
			lock.unlock();
		}

	}
}
