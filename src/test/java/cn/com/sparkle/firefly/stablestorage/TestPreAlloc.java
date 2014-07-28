package cn.com.sparkle.firefly.stablestorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TestPreAlloc {
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("c:/aaa"), "rw")
		;
		raf.setLength(1024 * 1024 * 1000);
		raf.close();
	}
}
