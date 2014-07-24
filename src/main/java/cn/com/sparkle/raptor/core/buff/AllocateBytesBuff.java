package cn.com.sparkle.raptor.core.buff;

import java.nio.ByteBuffer;

public class AllocateBytesBuff extends AbstractIoBuffer {
	public AllocateBytesBuff(int capacity, boolean isDirectMem) {
		if (!isDirectMem) {
			bb = ByteBuffer.allocate(capacity);
		} else {
			bb = ByteBuffer.allocateDirect(capacity);
		}
	}

	@Override
	public void close() {
	}
}
