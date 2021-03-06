package cn.com.sparkle.firefly.stablestorage.io;

import java.io.IOException;
import java.util.concurrent.Callable;

public interface RecordFileOut {
	/**
	 * 
	 * @param size
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeInt(int size, Callable<Object> callable,boolean isSync) throws IOException;

	/**
	 * 
	 * @param v
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long writeLong(long v, Callable<Object> callable,boolean isSync) throws IOException;
	/**
	 * 
	 * @param buf
	 * @param off
	 * @param length
	 * @param callable
	 * @return the start position of this data in file
	 * @throws IOException
	 */
	public long write(byte[] buf, int off, int length, Callable<Object> callable,boolean isSync) throws IOException;
	
	public void skip(int n);
	
	public void close() throws IOException;
}
