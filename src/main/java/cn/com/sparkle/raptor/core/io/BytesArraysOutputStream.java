package cn.com.sparkle.raptor.core.io;

import java.io.IOException;
import java.io.OutputStream;

public class BytesArraysOutputStream extends OutputStream{
	private byte[][] target;
	
	private int idx = 0;
	private int writeIdx = 0;
	
	public BytesArraysOutputStream(byte[][] target) {
		super();
		this.target = target;
	}

	public byte[][] getTarget() {
		return target;
	}



	@Override
	public void write(int arg0) throws IOException {
		target[idx][writeIdx] = (byte)arg0;
		++writeIdx;
		checkNext();
	}
	@Override
	public void write(byte[] arg0) throws IOException {
		write(arg0,0,arg0.length);
	}
	@Override
	public void write(byte[] arg0, int offset, int len) throws IOException {
		while(len != 0){
			int canWrite = Math.min(target[idx].length - writeIdx, len);
			System.arraycopy(arg0, offset, target[idx], writeIdx, canWrite);
			offset += canWrite;
			writeIdx += canWrite;
			len -= canWrite;
			checkNext();
		}
	}
	
	private void checkNext(){
		if(target[idx].length == writeIdx){
			++idx;
			writeIdx = 0;
		}
	}
}
