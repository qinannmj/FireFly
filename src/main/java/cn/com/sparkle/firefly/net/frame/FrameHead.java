package cn.com.sparkle.firefly.net.frame;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;

public class FrameHead {

	private final static Logger logger = Logger.getLogger(FrameHead.class);
	private int checksumType;

	private byte[] head = new byte[4];
	private byte[] checksum;

	private Integer bodySerializeSize; //for cache

	public FrameHead(int checksumType, int bodySize) throws UnsupportedChecksumAlgorithm {
		this.checksumType = checksumType;
		head[0] = (byte) ((bodySize >>> 24) & 0xFF | (checksumType << 4));
		head[1] = (byte) ((bodySize >>> 16) & 0xFF);
		head[2] = (byte) ((bodySize >>> 8) & 0xFF);
		head[3] = (byte) ((bodySize >>> 0) & 0xFF);
		checksum = ChecksumUtil.checksum(checksumType, head, 0, 4);
	}

	public FrameHead(byte[] head, byte[] checksum) {
		this.checksum = checksum;
		this.checksumType = (head[0] >> 4) & ChecksumUtil.MASK;
		this.head = head;
	}

	public int getChecksumType() {
		return checksumType;
	}

	public int getBodySize() {
		return ((head[0] & 15) << 24) + ((head[1] & 255) << 16) + ((head[2] & 255) << 8) + ((head[3] & 255) << 0);
	}

	public int getBodyChecksumLength() {
		return ChecksumUtil.checksumLength(this.checksumType, getBodySize());
	}

	public int getBodySerializeSize() {
		if (bodySerializeSize == null) {
			bodySerializeSize = getBodySize() + getBodyChecksumLength();
		}
		return bodySerializeSize;
	}

	public int getSerializeSize() {
		return head.length + checksum.length;
	}

	public boolean isValid() {
		try {
			return ChecksumUtil.validate(checksumType, head, checksum, 0, 4);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.warn("May be error program logic error or data be damaged!", e);
			return false;
		}
	}

	public byte[] getHead() {
		return head;
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public final static int calcChecksumLength(byte b) {
		int type = (b >> 4) & ChecksumUtil.MASK;
		return ChecksumUtil.checksumLength(type, 4);
	}
}
