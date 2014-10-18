package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

public class WriteBufferPackage implements BufferPackage{
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(WriteBufferPackage.class);
	
	byte[] buff;
	LinkedList<Callable<Object>> callAbleList = new LinkedList<Callable<Object>>();
	int used = 0;
	
	public WriteBufferPackage(int size) {
		this.buff = new byte[size];
	}
	
	@Override
	public boolean isFull(){
		return used == buff.length;
	}
	@Override
	public int size() {
		return used;
	}
	@Override
	public byte[] getBuffer() {
		return buff;
	}

	@Override
	public void clear() {
		this.used = 0;
		this.callAbleList = new LinkedList<Callable<Object>>();
	}

	@Override
	public List<Callable<Object>> getCallableList() {
		return callAbleList;
	}

	@Override
	public int write(byte[] data, int offset, int length) {
		int canWrite = length > buff.length - used ? buff.length - used : length;
		System.arraycopy(data, offset, buff, used, canWrite);
		used += canWrite;
		return canWrite;
	}
}
