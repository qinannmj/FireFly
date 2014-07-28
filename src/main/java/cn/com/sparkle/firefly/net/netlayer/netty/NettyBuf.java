package cn.com.sparkle.firefly.net.netlayer.netty;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

import cn.com.sparkle.firefly.net.netlayer.buf.Buf;

public class NettyBuf implements Buf {
	private ByteBuf byteBuf;
	private ByteBuffer byteBuffer;

	public NettyBuf(ByteBuf byteBuf) {
		this.byteBuf = byteBuf;
		byteBuffer = byteBuf.nioBuffer();
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	@Override
	public Buf duplicateBuf() {
		ByteBuf b = byteBuf.duplicate().retain();
		return new NettyBuf(b);
	}

	public ByteBuf getByteBuf() {
		return byteBuf;
	}

	@Override
	public void close() {
		byteBuf.release();
	}
}
