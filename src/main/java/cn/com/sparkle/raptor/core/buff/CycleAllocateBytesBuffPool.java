package cn.com.sparkle.raptor.core.buff;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;

public class CycleAllocateBytesBuffPool implements BuffPool {
	private Logger logger = Logger.getLogger(CycleAllocateBuff.class);

	protected MaximumSizeArrayCycleQueue<CycleBuff> queue;
	private int cellCapacity;
	private int totalCellSize;
	private int heapAllocateCount = 0;

	private ReentrantLock lock = new ReentrantLock();

	public CycleAllocateBytesBuffPool(int totalCellSize, int cellCapacity) {
		this.cellCapacity = cellCapacity;
		this.totalCellSize = totalCellSize;
		queue = new MaximumSizeArrayCycleQueue<CycleBuff>(CycleBuff.class, totalCellSize);

		for (int i = 0; i < totalCellSize; i++) {
			try {
				queue.push(new CycleAllocateBuff(this, cellCapacity, true));
			} catch (QueueFullException e) {
				e.printStackTrace();
			}
		}

	}

	public boolean close(CycleBuff buff) {
		try {
			lock.lock();
			if (buff.getPool() != this) {
				return false;
			}
			buff.getByteBuffer().clear();
			queue.push(buff);
			return true;
		} catch (QueueFullException e) {
			return false;
		} finally {
			lock.unlock();
		}
	}

	public IoBuffer get() {
		IoBuffer buff = queue.peek();
		if (buff != null) {
			queue.poll();
			heapAllocateCount = 0;
		} else {
			buff = new AllocateBytesBuff(cellCapacity, false);
			if (++heapAllocateCount == 100) {
				logger.warn("allocate a heap bytes,maybe you need to incread the size of pool!");
			}
		}
		return buff;
	}

	@Override
	public IoBufferArray get(int byteSize) {
		int size = byteSize / cellCapacity + (byteSize % cellCapacity == 0 ? 0 : 1);

		IoBuffer[] buff = new IoBuffer[size];
		for (int i = 0; i < size; i++) {
			buff[i] = queue.peek();
			if (buff[i] != null) {
				queue.poll();
				heapAllocateCount = 0;
			} else {
				buff[i] = new AllocateBytesBuff(cellCapacity, false);
				if (++heapAllocateCount == 100) {
					logger.warn("allocate a heap bytes,maybe you need to incread the size of pool!");
				}
			}
		}
		return new IoBufferArray(buff);
	}

	public int size() {
		return queue.size();
	}

	public int getCellCapacity() {
		return cellCapacity;
	}

	public int getTotalCellSize() {
		return totalCellSize;
	}

}
