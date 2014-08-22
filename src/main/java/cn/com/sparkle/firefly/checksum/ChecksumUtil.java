package cn.com.sparkle.firefly.checksum;

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

	public static byte[] checksum(int algorithmCode, byte[] bytes, int offset, int length) throws UnsupportedChecksumAlgorithm {
		if (algorithmCode == NO_CHECKSUM) {
			return nullChecksum;
		}
		int checksumLength = checksumLength(algorithmCode, length);
		byte[] result = new byte[checksumLength];
		Checksum checksum = getCheckSumAlgorithm(algorithmCode);
		for (int i = 0; i < checksumLength; i += 8) {
			int arrayLength = length < 512 ? length : 512;
			checksum.update(bytes, offset, arrayLength);
			long chunkChecksum = checksum.getValue();
			result[i] = (byte) (0xff & (chunkChecksum >> 56));
			result[i + 1] = (byte) (0xff & (chunkChecksum >> 48));
			result[i + 2] = (byte) (0xff & (chunkChecksum >> 40));
			result[i + 3] = (byte) (0xff & (chunkChecksum >> 32));
			result[i + 4] = (byte) (0xff & (chunkChecksum >> 24));
			result[i + 5] = (byte) (0xff & (chunkChecksum >> 16));
			result[i + 6] = (byte) (0xff & (chunkChecksum >> 8));
			result[i + 7] = (byte) (0xff & chunkChecksum);
			offset += arrayLength;
			length -= arrayLength;
			checksum.reset();
		}
		return result;
	}

	public static boolean validate(int algorithmCode, byte[] bytes, byte[] checksum, int offset, int length) throws UnsupportedChecksumAlgorithm {
		if (algorithmCode == NO_CHECKSUM) {
			return true;
		}
		int checksumLength = checksumLength(algorithmCode, length);
		Checksum _checksum = getCheckSumAlgorithm(algorithmCode);
		for (int i = 0; i < checksumLength; i += 8) {
			int arrayLength = length < 512 ? length : 512;
			_checksum.update(bytes, offset, arrayLength);
			long chunkChecksum = _checksum.getValue();

			if (chunkChecksum != (((long) checksum[i] << 56) + ((long) (checksum[i + 1] & 255) << 48) + ((long) (checksum[i + 2] & 255) << 40)
					+ ((long) (checksum[i + 3] & 255) << 32) + ((long) (checksum[i + 4] & 255) << 24) + ((checksum[i + 5] & 255) << 16)
					+ ((checksum[i + 6] & 255) << 8) + ((checksum[i + 7] & 255) << 0))) {
				return false;
			}
			offset += arrayLength;
			length -= arrayLength;
			_checksum.reset();
		}
		return true;
	}

	public final static class UnsupportedChecksumAlgorithm extends Exception {
		private static final long serialVersionUID = 8360239510990544038L;

		public UnsupportedChecksumAlgorithm(String s) {
			super(s);
		}
	}
}
