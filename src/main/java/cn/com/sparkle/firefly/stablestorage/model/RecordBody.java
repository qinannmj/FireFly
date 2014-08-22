package cn.com.sparkle.firefly.stablestorage.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;

/**
 * 
 * -------------------------------------------------------------------------------------------------------------------------------------
 * | body bytes      | ceil(body length / ChecksumUtil.CHECK_CHUNK_SIZE) * 8 or 0 bytes body checksum ,checksum type is defined in head    |
 * -------------------------------------------------------------------------------------------------------------------------------------
 * 
 * @author qinan.qn
 *
 */
public final class RecordBody {
	private final static Logger logger = Logger.getLogger(RecordBody.class);
	private byte[] body;
	private byte[] checksum;
	private int checksumType;

	public RecordBody(byte[] body, int checksumType) throws UnsupportedChecksumAlgorithm {
		this.checksumType = checksumType;
		this.body = body;
		checksum = ChecksumUtil.checksum(checksumType, body, 0, body.length);
	}

	public RecordBody(byte[] body, byte[] checksum, int checksumType) {
		this.body = body;
		this.checksum = checksum;
		this.checksumType = checksumType;
	}

	public boolean isValid() {
		try {
			return ChecksumUtil.validate(checksumType, body, checksum, 0, body.length);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.warn("May be error program logic error or data be damaged!", e);
			return false;
		}
	}

	public byte[] getBody() {
		return body;
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public void writeToStream(RecordFileOut out, Callable<Object> callable,boolean isSync) throws IOException {
		out.write(this.body, 0, this.body.length, null,false);
		out.write(this.checksum, 0, this.checksum.length, callable,isSync);
	}
	
	public int getSerializeSize(){
		return body.length + this.checksum.length;
	}
	public static RecordBody readFromStream(InputStream in, RecordHead head) throws IOException {
		byte[] body = new byte[head.getBodySize()];
		int size = in.read(body);
		if (size != head.getBodySize()) {
			return null;
		}
		byte[] checksum = new byte[head.getBodyChecksumLength()];
		size = in.read(checksum);
		if (size != head.getBodyChecksumLength()) {
			return null;
		}
		return new RecordBody(body, checksum, head.getChecksumType());
	}
}
