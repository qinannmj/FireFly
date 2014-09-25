package cn.com.sparkle.raptor.core.buff;

import java.nio.ByteBuffer;

public class QuoteBytesBuff extends AbstractIoBuffer {
	public QuoteBytesBuff(byte[] bytes, int offset, int length) {
		bb = ByteBuffer.wrap(bytes, offset, length);
	}

	@Override
	public void close() {
	}
}
