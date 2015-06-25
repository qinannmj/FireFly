package cn.com.sparkle.firefly.net.frame;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;

public class FrameBody {
	private final static Logger logger = Logger.getLogger(FrameBody.class);
	
	private int checksumType;
	private byte[][] body;
	private byte[] checksum;
	private	int bodyLength = 0;
	private FrameHead head;

	public FrameBody(byte[][] body, byte[] checksum, int checksumType) {
		this.body = body;
		this.checksum = checksum;
		this.checksumType = checksumType;
		for(byte[] b : body){
			bodyLength += b.length;
		}
	}

	public FrameBody(byte[][] body, int checksumType) throws UnsupportedChecksumAlgorithm {
		this.checksumType = checksumType;
		this.body = body;
		for(byte[] b : body){
			bodyLength += b.length;
		}
		checksum = ChecksumUtil.checksum(checksumType, body, bodyLength);
	}

	public boolean isValid() {
		try {
			return ChecksumUtil.validate(checksumType, body, checksum, bodyLength);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.warn("May be error program logic error or data be damaged!", e);
			return false;
		}
	}

	public FrameHead getHead() {
		return head;
	}

	public void setHead(FrameHead head) {
		this.head = head;
	}

	public int getBodySize() {
		return bodyLength;
	}

	public int getSerializeSize() {
		return bodyLength + checksum.length;
	}

	public int getChecksumType() {
		return checksumType;
	}

	public byte[][] getBody() {
		return body;
	}

	public byte[] getChecksum() {
		return checksum;
	}
}
