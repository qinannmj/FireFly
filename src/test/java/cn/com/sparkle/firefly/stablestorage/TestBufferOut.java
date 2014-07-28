package cn.com.sparkle.firefly.stablestorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import cn.com.sparkle.firefly.stablestorage.io.BufferedFileOut;

public class TestBufferOut {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		final BufferedFileOut out = new BufferedFileOut(new RandomAccessFile("d://jbpaxos//a.test1", "rws"));
		out.init(false,10 * 1024 * 1024 , 10);
		Thread t = new Thread(){
			public void run(){
				byte[] buf = new byte[128];
//				byte[] buf = new byte[1024 * 1024 * 8];
				while(true){
					try {
						out.write(buf, 0, buf.length, null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
	}
}
