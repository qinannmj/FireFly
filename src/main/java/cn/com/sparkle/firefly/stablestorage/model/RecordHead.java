package cn.com.sparkle.firefly.stablestorage.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;

/**
 * ------------------------------------------------------------------------------------------------------
 * |  4bytes = record type(1bit) + checksum type(4bit) + bodylength(27bit)|8 instanceId  | 8byte checksum |
 * ------------------------------------------------------------------------------------------------------
 * 
 * chechsum may is java in-build CRC32 , ALDER32 or PURE JAVA CRC32
 * 
 * For compatibility , the checksum type be used to hint the type of checksum.
 * 
 * @author qinan.qn
 *
 */
public final class RecordHead {

	private final static Logger logger = Logger.getLogger(RecordHead.class);
	private byte[] head;
	private byte[] checksum;
	private Integer bodySize = null;
	private Integer bodyChecksumLength = null;
	private Long instanceId = null;
	private RecordType type = null;
	private int checksumType;

	public RecordHead(int bodySize, long instanceId, RecordType type, int checksumType) throws UnsupportedChecksumAlgorithm {
		this.checksumType = checksumType;
		head = new byte[12];
		head[0] = (byte) ((bodySize >>> 24) & 0xFF | type.getValue() | (checksumType << 3));
		head[1] = (byte) ((bodySize >>> 16) & 0xFF);
		head[2] = (byte) ((bodySize >>> 8) & 0xFF);
		head[3] = (byte) ((bodySize >>> 0) & 0xFF);
		head[4] = (byte) (0xff & (instanceId >> 56));
		head[5] = (byte) (0xff & (instanceId >> 48));
		head[6] = (byte) (0xff & (instanceId >> 40));
		head[7] = (byte) (0xff & (instanceId >> 32));
		head[8] = (byte) (0xff & (instanceId >> 24));
		head[9] = (byte) (0xff & (instanceId >> 16));
		head[10] = (byte) (0xff & (instanceId >> 8));
		head[11] = (byte) (0xff & instanceId);
		checksum = ChecksumUtil.checksum(checksumType, head, 0, 12);
		this.bodySize = bodySize;
		this.type = type;
		this.instanceId = instanceId;
	}

	public RecordHead(byte[] head, byte[] checksum) {
		this.head = head;
		this.checksum = checksum;
		checksumType = (head[0] >> 3) & ChecksumUtil.MASK;
	}

	public boolean isValid() {
		try {
			return ChecksumUtil.validate(checksumType, head, checksum, 0, 12);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.warn("May be error program logic error or data be damaged!", e);
			return false;
		}
	}

	public int getBodySize() {
		if (bodySize == null) {
			bodySize = ((head[0] & 7) << 24) + ((head[1] & 255) << 16) + ((head[2] & 255) << 8) + ((head[3] & 255) << 0);
		}
		return bodySize;
	}

	public int getBodyChecksumLength() {
		if (bodyChecksumLength == null) {
			bodyChecksumLength = ChecksumUtil.checksumLength(this.checksumType, getBodySize());
		}
		return bodyChecksumLength;
	}

	public long getInstanceId() {
		if (instanceId == null) {
			instanceId = ((long) head[4] << 56) + ((long) (head[5] & 255) << 48) + ((long) (head[6] & 255) << 40) + ((long) (head[7] & 255) << 32)
					+ ((long) (head[8] & 255) << 24) + ((head[9] & 255) << 16) + ((head[10] & 255) << 8) + ((head[11] & 255) << 0);
		}
		return instanceId;
	}

	public RecordType getType() {
		if (type == null) {
			if ((head[0] & 0x80) == RecordType.SUCCESS.getValue()) {
				type = RecordType.SUCCESS;
			} else {
				type = RecordType.VOTE;
			}
		}
		return type;
	}

	public int getSerializeSize() {
		return head.length + checksum.length;
	}

	public byte[] getBytes() {
		return head;
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public int getChecksumType() {
		return checksumType;
	}

	public static RecordHead readFromStream(InputStream inputStream) throws IOException {
		byte[] head = new byte[12];
		int size = inputStream.read(head);
		if (size == head.length) {
			byte[] checksum = new byte[ChecksumUtil.checksumLength((head[0] >> 3) & ChecksumUtil.MASK, 12)];
			size = inputStream.read(checksum);
			if (size == checksum.length) {
				RecordHead rh = new RecordHead(head, checksum);
				if(rh.isValid() && rh.getBodySize() == 0){
					return null;//write normally
				}else{
					return rh;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public long writeToStream(RecordFileOut out, Callable<Object> callable,boolean isSync) throws IOException {
		long start = out.write(this.head, 0, 12, null,false);
		out.write(this.checksum, 0, this.checksum.length, callable,isSync);
		return start;
	}
}
