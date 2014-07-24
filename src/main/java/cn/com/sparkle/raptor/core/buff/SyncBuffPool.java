package cn.com.sparkle.raptor.core.buff;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SyncBuffPool extends CycleAllocateBytesBuffPool {
	public SyncBuffPool(int totalCellSize, int cellCapacity) {
		super(totalCellSize, cellCapacity);
	}

	private ReentrantLock lock = new ReentrantLock();
	private Condition empty = lock.newCondition();

	@Override
	public void close(CycleBuff buff) {
		lock.lock();
		try {
			super.close(buff);
			empty.signal();
		} finally {
			lock.unlock();
		}

	}

	@Override
	public IoBufferArray tryGet(int byteSize) {
		lock.lock();
		try {
			if (byteSize < 1) {
				return new IoBufferArray(new IoBuffer[0]);
			}
			return super.tryGet(byteSize);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public CycleBuff tryGet() {
		lock.lock();
		try {
			return super.tryGet();
		} finally {
			lock.unlock();
		}

	}
}
