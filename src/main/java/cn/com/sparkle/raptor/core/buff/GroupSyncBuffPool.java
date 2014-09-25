package cn.com.sparkle.raptor.core.buff;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class GroupSyncBuffPool implements BuffPool {
	private Logger logger = Logger.getLogger(GroupSyncBuffPool.class);
	
	private String poolName;
	private AtomicInteger getIdx = new AtomicInteger(0);

	private SingleSyncBuffPool[] poolGroup = new SingleSyncBuffPool[32];
	private int cellCapacity;

	private AtomicInteger heapAllocateCount = new AtomicInteger(0);

	public GroupSyncBuffPool(int totalCellSize, int cellCapacity,String poolName) {
		this.cellCapacity = cellCapacity;
		this.poolName = poolName;
		int everyCellSize = (int) Math.ceil(((double) totalCellSize) / poolGroup.length);
		for (int i = 0; i < poolGroup.length; ++i) {
			poolGroup[i] = new SingleSyncBuffPool(everyCellSize, cellCapacity,poolName);
		}
	}

	@Override
	public boolean close(CycleBuff buff) {
		throw new RuntimeException("unsupport method");
	}

	@Override
	public IoBufferArray get(int byteSize) {
		int size = byteSize / cellCapacity + (byteSize % cellCapacity == 0 ? 0 : 1);
		IoBuffer[] buff = new IoBuffer[size];
		for (int i = 0; i < size; i++) {
			buff[i] = get();
		}
		return new IoBufferArray(buff);
	}

	@Override
	public IoBuffer get() {
		int idx = nextGetIdx();
		IoBuffer buff = poolGroup[idx].get();
		if (buff instanceof CycleBuff) {
			heapAllocateCount.set(0);
		} else {
			if (heapAllocateCount.incrementAndGet() == 100) {
				logger.warn(String.format("[%s]allocate a heap bytes,maybe you need to incread the size of pool,size %s idx[%s]!", this.poolName,poolGroup[idx].size(),idx));
			}
		}
		return buff;
	}

	@Override
	public int size() {
		int size = 0;
		for (BuffPool pool : poolGroup) {
			size += pool.size();
		}
		return size;
	}

	private int nextGetIdx() {
		while (true) {
			int old = getIdx.get();
			int newV = (old + 1) % poolGroup.length;
			if (getIdx.compareAndSet(old, newV)) {
				return newV;
			}
		}
	}
}
 