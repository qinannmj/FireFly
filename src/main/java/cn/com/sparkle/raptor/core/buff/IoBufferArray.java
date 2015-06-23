package cn.com.sparkle.raptor.core.buff;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class IoBufferArray {
	private IoBuffer[] ioBuffArray;
	private final static Logger logger = Logger.getLogger(IoBufferArray.class);

	public IoBufferArray(IoBuffer[] ioBuffArray) {
		this.ioBuffArray = ioBuffArray;
	}

	public IoBuffer[] getIoBuffArray() {
		return ioBuffArray;
	}

	public void put(byte[] src, int offset, int length) {
		for (int flag = 0; flag < ioBuffArray.length; flag++) {
			ByteBuffer byteBuffer = ioBuffArray[flag].getByteBuffer();
			int canWriteLength = length > byteBuffer.remaining() ? byteBuffer.remaining() : length;
			try {
				byteBuffer.put(src, offset, canWriteLength);
			} catch (java.lang.RuntimeException e) {
				if(logger.isDebugEnabled()){
					logger.debug(src.length + "  " + offset + "   " + canWriteLength + "   " + byteBuffer.remaining());
				}
				throw e;
			}
			length -= canWriteLength;
			if (length == 0)
				return;
			offset += canWriteLength;
		}
	}

	public void put(byte b) {
		byte[] bs = new byte[1];
		bs[0] = b;
		put(bs, 0, 1);
	}

	public void put(byte[] b) {
		put(b, 0, b.length);
	}
}
