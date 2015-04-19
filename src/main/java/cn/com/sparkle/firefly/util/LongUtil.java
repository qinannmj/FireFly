package cn.com.sparkle.firefly.util;

public class LongUtil {
	public final static void toByte(long value, byte[] buff, int offset) {
		buff[offset] = (byte) (0xff & (value >> 56));
		buff[offset + 1] = (byte) (0xff & (value >> 48));
		buff[offset + 2] = (byte) (0xff & (value >> 40));
		buff[offset + 3] = (byte) (0xff & (value >> 32));
		buff[offset + 4] = (byte) (0xff & (value >> 24));
		buff[offset + 5] = (byte) (0xff & (value >> 16));
		buff[offset + 6] = (byte) (0xff & (value >> 8));
		buff[offset + 7] = (byte) (0xff & value);
	}

	public final static long toLong(byte[] buff, int offset) {
		return ((long) buff[offset] << 56) + ((long) (buff[offset + 1] & 255) << 48) + ((long) (buff[offset + 2] & 255) << 40)
				+ ((long) (buff[offset + 3] & 255) << 32) + ((long) (buff[offset + 4] & 255) << 24) + ((buff[offset + 5] & 255) << 16)
				+ ((buff[offset + 6] & 255) << 8) + ((buff[offset + 7] & 255) << 0);
	}
	public static void main(String[] args) {
		long v = 60684;
		byte[] b = new byte[8];
		toByte(v, b, 0);
		System.out.println(toLong(b, 0));
	}
}
