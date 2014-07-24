package cn.com.sparkle.paxos.net.netlayer.buf;

import java.nio.ByteBuffer;

public class ReferenceBuf implements Buf {
	ByteBuffer buffer;

	public ReferenceBuf(byte[] buff) {
		buffer = ByteBuffer.wrap(buff);
	}

	public ReferenceBuf(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return buffer;
	}

	@Override
	public Buf duplicateBuf() {
		return new ReferenceBuf(buffer.duplicate());
	}

	@Override
	public void close() {
	}
}
