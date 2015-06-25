package cn.com.sparkle.firefly.util;

public class BytesArrayMaker {
	private final static int MAX_CHUNK = 8 * 1024;
	public final static byte[][] makeBytesArray(int byteLen){
		int remained = byteLen % MAX_CHUNK;
		int remainedSize = remained == 0 ? 0 : 1;
		int chunkSize = byteLen / MAX_CHUNK;
		byte[][] bytes = new byte[chunkSize + remainedSize][];
		for (int i = 0; i < chunkSize;++i){
			bytes[i] = new byte[MAX_CHUNK];
		}
		if(remained != 0){
			bytes[bytes.length - 1] = new byte[remained];
		}
		return bytes;
	}
}
