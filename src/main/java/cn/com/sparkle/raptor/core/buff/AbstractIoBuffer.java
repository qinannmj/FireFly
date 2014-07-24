package cn.com.sparkle.raptor.core.buff;

import java.nio.ByteBuffer;

public abstract class AbstractIoBuffer implements IoBuffer {
	protected ByteBuffer bb;

	public ByteBuffer getByteBuffer() {
		return bb;
	};
}
