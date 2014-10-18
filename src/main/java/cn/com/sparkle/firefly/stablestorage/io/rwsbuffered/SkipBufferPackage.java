package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class SkipBufferPackage implements BufferPackage{
	private int skips;
	private final static LinkedList<Callable<Object>> callAbleList = new LinkedList<Callable<Object>>();
	public SkipBufferPackage(int skips){
		this.skips = skips;
	}
	@Override
	public boolean isFull() {
		return true;
	}

	@Override
	public int size() {
		return skips;
	}

	@Override
	public byte[] getBuffer() {
		return null;
	}

	@Override
	public void clear() {
	}

	@Override
	public List<Callable<Object>> getCallableList() {
		return this.callAbleList;
	}

	@Override
	public int write(byte[] buff, int offset, int length) {
		throw new RuntimeException("unsupported method!");
	}

}
