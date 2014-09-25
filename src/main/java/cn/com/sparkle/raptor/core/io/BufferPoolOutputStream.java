package cn.com.sparkle.raptor.core.io;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;

/**
 * This outputStream is not thread's safe.
 */
public class BufferPoolOutputStream extends OutputStream {

	private int writeCount = 0;
	private BuffPool pool;
	private IoBuffer cycleBuff;
	private LinkedList<IoBuffer> arrayList = new LinkedList<IoBuffer>();
	private int reserverPosition = -1;
	private int reserveSize;

	public BufferPoolOutputStream(BuffPool pool) {
		this(pool, 0, null);
	}

	public BufferPoolOutputStream(BuffPool pool, int reserveSize, IoBuffer ioBuffer) {
		this.reserveSize = reserveSize;
		this.pool = pool;
		if (ioBuffer != null) {
			cycleBuff = ioBuffer;
		} else {
			cycleBuff = pool.get();
		}
		arrayList.add(cycleBuff);
		while (reserveSize > cycleBuff.getByteBuffer().remaining()) {
			reserveSize -= cycleBuff.getByteBuffer().remaining();
			reserverPosition = cycleBuff.getByteBuffer().position();
			cycleBuff.getByteBuffer().position(cycleBuff.getByteBuffer().capacity());
			cycleBuff = pool.get();
			arrayList.add(cycleBuff);
		}
		if (reserverPosition == -1) {
			reserverPosition = cycleBuff.getByteBuffer().position();
		}
		cycleBuff.getByteBuffer().position(cycleBuff.getByteBuffer().position() + reserveSize);
	}

	@Override
	public void write(int b) {
		if (cycleBuff.getByteBuffer().remaining() == 0) {
			cycleBuff = pool.get();
			arrayList.add(cycleBuff);
		}
		cycleBuff.getByteBuffer().put((byte) b);
		++writeCount;
	}

	public void write(IoBuffer buff) {
		while (buff.getByteBuffer().hasRemaining()) {
			if (cycleBuff.getByteBuffer().remaining() == 0) {
				cycleBuff = pool.get();
				arrayList.add(cycleBuff);
			}
			int len = buff.getByteBuffer().remaining();
			int canWrite = Math.min(cycleBuff.getByteBuffer().remaining(), len);

			int oldLimit = buff.getByteBuffer().limit();
			buff.getByteBuffer().limit(buff.getByteBuffer().position() + canWrite);
			cycleBuff.getByteBuffer().put(buff.getByteBuffer());
			buff.getByteBuffer().limit(oldLimit);
			writeCount += canWrite;
		}
	}

	@Override
	public void write(byte[] b, int off, int len) {
		while (len > 0) {
			if (cycleBuff.getByteBuffer().remaining() == 0) {
				cycleBuff = pool.get();
				arrayList.add(cycleBuff);
			}
			int canWrite = Math.min(cycleBuff.getByteBuffer().remaining(), len);
			cycleBuff.getByteBuffer().put(b, off, canWrite);
			len -= canWrite;
			off += canWrite;
			writeCount += canWrite;
		}
	}

	public void writeReserve(byte[] b, int off, int len) {
		int writeLength = len > b.length - off ? b.length - off : len;
		if (writeLength > reserveSize) {
			throw new RuntimeException("write count of byte more than the size of reservation");
		}
		int pos = reserverPosition;
		for (IoBuffer buff : arrayList) {
			int canWrite;
			canWrite = Math.min(buff.getByteBuffer().capacity() - pos, writeLength);
			writeLength -= canWrite;
			for (int i = 0; i < canWrite; i++) {
				buff.getByteBuffer().put(pos + i, b[off]);
				++off;
			}
			pos = 0;
			if (writeLength == 0) {
				break;
			}
		}
	}

	public List<IoBuffer> getBuffArray() {
		return arrayList;
	}

	public int getWriteCount() {
		return writeCount;
	}
}
