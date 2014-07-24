package cn.com.sparkle.raptor.core.buff;

import java.nio.ByteBuffer;

public interface IoBuffer {
	public ByteBuffer getByteBuffer();

	public void close();
}
