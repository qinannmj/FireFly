package cn.com.sparkle.paxos.net.frame;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.checksum.ChecksumUtil;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;

public class FrameBody {
	private final static Logger logger = Logger.getLogger(FrameBody.class);

	private int checksumType;
	private byte[] body;
	private byte[] checksum;

	public FrameBody(byte[] body, byte[] checksum, int checksumType) {
		this.body = body;
		this.checksum = checksum;
		this.checksumType = checksumType;
	}

	public FrameBody(byte[] body, int checksumType) throws UnsupportedChecksumAlgorithm {
		this.checksumType = checksumType;
		this.body = body;
		checksum = ChecksumUtil.checksum(checksumType, body, 0, body.length);
	}

	public boolean isValid() {
		try {
			return ChecksumUtil.validate(checksumType, body, checksum, 0, body.length);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.warn("May be error program logic error or data be damaged!", e);
			return false;
		}
	}

	public int getBodySize() {
		return body.length;
	}

	public int getSerializeSize() {
		return body.length + checksum.length;
	}

	public int getChecksumType() {
		return checksumType;
	}

	public byte[] getBody() {
		return body;
	}

	public byte[] getChecksum() {
		return checksum;
	}
}
