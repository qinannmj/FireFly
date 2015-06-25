package cn.com.sparkle.raptor.core.io;

import java.io.IOException;
import java.io.InputStream;

import cn.com.sparkle.raptor.core.buff.IoBuffer;

/**
 * This inputStream is not thread's safe.
 */
public class IoBufferArrayInputStream extends InputStream {

	protected IoBuffer[] buff;
	protected int capacity = 0;
	protected int curArrayPos = 0;

	public IoBufferArrayInputStream(IoBuffer[] buff) {
		this(buff, Integer.MAX_VALUE);
	}

	public IoBufferArrayInputStream(IoBuffer[] buff, int length) {
		this.buff = buff;
		this.capacity = length;
	}

	private void checkCurByteBuffer() {
		for (; curArrayPos < buff.length && !buff[curArrayPos].getByteBuffer().hasRemaining(); ++curArrayPos)
			;
	}

	public int read() {
		if (capacity > 0) {
			--capacity;
			int r = buff[curArrayPos].getByteBuffer().get() & 0xff;
			checkCurByteBuffer();
			return r;
		} else
			return -1;
	}

	public int read(byte b[], int off, int len) {
		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		}
		if (capacity == 0) {
			return -1;
		}
		len = Math.min(capacity, len);
		capacity -= len;

		int needRead = len;
		while (needRead > 0) {
			int curAvailable = Math.min(needRead, buff[curArrayPos].getByteBuffer().remaining());
			buff[curArrayPos].getByteBuffer().get(b, off, curAvailable);
			off += curAvailable;
			needRead -= curAvailable;
			checkCurByteBuffer();
		}
		return len;
	}
	public int read(byte b[][]) throws IOException{
		int totalReadSize = 0;
		for(byte[] bs : b){
			int readSize = read(bs);
			if(readSize < 0){
				break;
			}else{
				totalReadSize += readSize;
				if(readSize < bs.length){
					break;
				}
			}
		}
		return totalReadSize;
	}

	public synchronized int available() {
		return capacity;
	}

	public void close() throws IOException {
	}
}
