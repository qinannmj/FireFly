package cn.com.sparkle.paxos.net.netlayer.raptor;

import java.nio.ByteBuffer;

import cn.com.sparkle.paxos.net.netlayer.buf.Buf;
import cn.com.sparkle.raptor.core.buff.CycleBuff;
import cn.com.sparkle.raptor.core.buff.IoBuffer;

public class RaptorBuf implements Buf {
	private IoBuffer buffer;
	private ByteBuffer byteBuffer;

	public RaptorBuf(IoBuffer buffer) {
		this.buffer = buffer;
		this.byteBuffer = buffer.getByteBuffer();
	}

	private RaptorBuf(IoBuffer buffer, ByteBuffer byteBuffer) {
		this.buffer = buffer;
		this.byteBuffer = byteBuffer;
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	@Override
	public Buf duplicateBuf() {
		RaptorBuf buf = new RaptorBuf(buffer, buffer.getByteBuffer().duplicate());
		if (buffer instanceof CycleBuff) {
			((CycleBuff) buffer).incRef();
		}
		return buf;
	}

	@Override
	public void close() {
		buffer.close();
	}

}
