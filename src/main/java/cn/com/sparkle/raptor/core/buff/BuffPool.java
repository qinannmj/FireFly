package cn.com.sparkle.raptor.core.buff;


public interface BuffPool {
	public boolean close(CycleBuff buff);

	public IoBufferArray get(int byteSize);

	public IoBuffer get();

	public int size();
}
