package cn.com.sparkle.raptor.core.buff;

public class CycleAllocateBuff extends AllocateBytesBuff implements CycleBuff {

	private volatile int refCnt = 1;

	BuffPool pool;

	public CycleAllocateBuff(BuffPool pool, int capacity, boolean isDirectMem) {
		super(capacity, isDirectMem);
		this.pool = pool;
	}

	@Override
	public synchronized void close() {
		if (pool != null) {
			if (refCnt == 1) {
				pool.close(this);
			} else {
				--refCnt;
			}
		}
	}

	@Override
	public BuffPool getPool() {
		return pool;
	}

	@Override
	public void incRef() {
		++refCnt;
	}
}
