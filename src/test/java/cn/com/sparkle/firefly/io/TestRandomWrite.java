package cn.com.sparkle.firefly.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TestRandomWrite {
	public static void main(String[] args) throws IOException {
		long time = System.nanoTime();
		File f = new File("c:/testio/randomwrite");
		if(f.exists()){
			f.delete();
		}
		RandomAccessFile raf = new RandomAccessFile(f, "rws");
		byte[] buff = new byte[1024];
		for(int i= 0 ; i < 10000 ; i++){
			raf.write(buff);
		}
		raf.close();
		System.out.println(System.nanoTime() - time);
	}
}
