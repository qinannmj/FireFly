package cn.com.sparkle.paxos.net.netlayer.netty;

import java.nio.ByteBuffer;

import cn.com.sparkle.paxos.net.netlayer.buf.Buf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;

public class ReferenceNettyByteBuf extends UnpooledHeapByteBuf implements Buf {

	public final static ReferenceNettyByteBuf refer(byte[] bytes) {
		return new ReferenceNettyByteBuf(bytes);
	}

	public ReferenceNettyByteBuf(byte[] initialArray) {
		super(UnpooledByteBufAllocator.DEFAULT, initialArray, initialArray.length);
	}

	@Override
	public ByteBuf capacity(int newCapacity) {
		throw new RuntimeException("The method of reference byte buf is not supported!");
	}

	@Override
	public ByteBuffer getByteBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Buf duplicateBuf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
}
