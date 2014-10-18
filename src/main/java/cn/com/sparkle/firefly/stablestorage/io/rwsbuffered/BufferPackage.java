package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.List;
import java.util.concurrent.Callable;

public interface BufferPackage {
	public boolean isFull();
	public int size();
	public byte[] getBuffer();
	public void clear();
	public List<Callable<Object>> getCallableList();
	public int write(byte[] buff,int offset,int length);
}
