package cn.com.sparkle.raptor.core.buff;

import java.io.IOException;

public interface BuffPool {
	public void close(CycleBuff buff);

	public IoBufferArray tryGet(int byteSize);

	public CycleBuff tryGet();

	public static class PoolEmptyException extends IOException {
		private static final long serialVersionUID = 3029731281015223053L;
	}

	public int size();
}
