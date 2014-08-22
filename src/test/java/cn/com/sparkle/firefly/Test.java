package cn.com.sparkle.firefly;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class Test {
	public static void main(String[] args) throws IOException {
		
		File f = new File("c:/test.data");
		if(!f.exists()){
			f.createNewFile();
		}
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		raf.writeByte(8);
		MappedByteBuffer mappedBuffer = raf.getChannel().map(MapMode.READ_ONLY, 0, 100);
		
	}
}
