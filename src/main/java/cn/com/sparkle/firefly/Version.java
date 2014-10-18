package cn.com.sparkle.firefly;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map.Entry;

public class Version {
	public final static String APP_VER = "0.0.5";
	public static void main(String[] args) {
		File f = new File("c:");
		RandomAccessFile raf;
		for(Entry<Object, Object> e : System.getProperties().entrySet()){
			System.out.println(e.getKey() + "=" + e.getValue());
		}
	}
}
