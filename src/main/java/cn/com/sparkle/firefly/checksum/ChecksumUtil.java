package cn.com.sparkle.firefly.checksum;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * 
 * @author qinan.qn
 *
 */
public final class ChecksumUtil {
	
	//the type less than 16 for only use 4 bits
	public final static int NO_CHECKSUM = 0;
	public final static int INBUILD_ALDER32 = 1;
	public final static int INBUILD_CRC32 = 2;
	public final static int PURE_JAVA_CRC32 = 3;
	public final static int MASK = 15;

	private final static int CHECK_CHUNK_SIZE = 512;

	private final static byte[] nullChecksum = new byte[0];

	public final static HashSet<String> COMPATIBALE_CHECKSUM_TYPE = new HashSet<String>();

	static {
		COMPATIBALE_CHECKSUM_TYPE.add("0");//no checksum
		COMPATIBALE_CHECKSUM_TYPE.add("1");//in-build alder32
		COMPATIBALE_CHECKSUM_TYPE.add("2"); // in-build crc32
		COMPATIBALE_CHECKSUM_TYPE.add("3"); //java pure crc32
	}

	public static Checksum getCheckSumAlgorithm(int algorithmCode) throws UnsupportedChecksumAlgorithm {
		switch (algorithmCode) {
		case NO_CHECKSUM:
			return new NoChecksum();
		case INBUILD_ALDER32:
			return new Adler32();
		case INBUILD_CRC32:
			return new CRC32();
		case PURE_JAVA_CRC32:
			return new PureJavaCrc32();
		default:
			throw new UnsupportedChecksumAlgorithm("unsupported checksum algorithm code:" + algorithmCode);

		}
	}

	public static int checksumLength(int algorithmCode, int dataLength) {
		if (algorithmCode == NO_CHECKSUM) {
			return 0;
		} else {
			return (int) Math.ceil((double) dataLength / CHECK_CHUNK_SIZE) * 8;
		}
	}

	public static byte[] checksum(int algorithmCode, byte[][] bytes, int length) throws UnsupportedChecksumAlgorithm {
		if (algorithmCode == NO_CHECKSUM) {
			return nullChecksum;
		}
		int checksumLength = checksumLength(algorithmCode, length);
		byte[] result = new byte[checksumLength];
		Checksum checksum = getCheckSumAlgorithm(algorithmCode);
		int idx = 0;
		int offset = 0;
		for (int i = 0; i < checksumLength; i += 8) {
			int arrayLength = Math.min(length, CHECK_CHUNK_SIZE);
			int remaind = arrayLength;
			while(remaind != 0){
				int calcSize = bytes[idx].length - offset;
				calcSize = Math.min(calcSize, remaind);
				checksum.update(bytes[idx], offset, calcSize);
				remaind-= calcSize;
				offset += calcSize;
				if(offset == bytes[idx].length){
					++idx;
					offset = 0;
				}
			}
			long chunkChecksum = checksum.getValue();
			result[i] = (byte) (0xff & (chunkChecksum >> 56));
			result[i + 1] = (byte) (0xff & (chunkChecksum >> 48));
			result[i + 2] = (byte) (0xff & (chunkChecksum >> 40));
			result[i + 3] = (byte) (0xff & (chunkChecksum >> 32));
			result[i + 4] = (byte) (0xff & (chunkChecksum >> 24));
			result[i + 5] = (byte) (0xff & (chunkChecksum >> 16));
			result[i + 6] = (byte) (0xff & (chunkChecksum >> 8));
			result[i + 7] = (byte) (0xff & chunkChecksum);
			length -= arrayLength;
			checksum.reset();
		}
		return result;
	}

	public static boolean validate(int algorithmCode, byte[][] bytes, byte[] checksum, int length) throws UnsupportedChecksumAlgorithm {
		byte[] waitCheck = checksum(algorithmCode,bytes,length);
		if (waitCheck == nullChecksum) {
			return true;
		}
		return Arrays.equals(waitCheck, checksum);
	}

	public final static class UnsupportedChecksumAlgorithm extends Exception {
		private static final long serialVersionUID = 8360239510990544038L;

		public UnsupportedChecksumAlgorithm(String s) {
			super(s);
		}
	}
}
