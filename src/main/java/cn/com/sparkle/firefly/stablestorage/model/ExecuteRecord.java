package cn.com.sparkle.firefly.stablestorage.model;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.zip.Checksum;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;

public class ExecuteRecord {
	private Long instanceId;
	private byte[] array;

	public ExecuteRecord(long instanceId) throws UnsupportedChecksumAlgorithm {
		this.instanceId = instanceId;
		array = new byte[16];
		array[0] = (byte) (0xff & (instanceId >> 56));
		array[1] = (byte) (0xff & (instanceId >> 48));
		array[2] = (byte) (0xff & (instanceId >> 40));
		array[3] = (byte) (0xff & (instanceId >> 32));
		array[4] = (byte) (0xff & (instanceId >> 24));
		array[5] = (byte) (0xff & (instanceId >> 16));
		array[6] = (byte) (0xff & (instanceId >> 8));
		array[7] = (byte) (0xff & instanceId);

		long checksum = checksum(array, 0, 8);

		array[8] = (byte) (0xff & (checksum >> 56));
		array[9] = (byte) (0xff & (checksum >> 48));
		array[10] = (byte) (0xff & (checksum >> 40));
		array[11] = (byte) (0xff & (checksum >> 32));
		array[12] = (byte) (0xff & (checksum >> 24));
		array[13] = (byte) (0xff & (checksum >> 16));
		array[14] = (byte) (0xff & (checksum >> 8));
		array[15] = (byte) (0xff & checksum);
	}

	public ExecuteRecord(byte[] array) {
		this.array = array;
	}

	public byte[] getBytes() {
		return array;
	}

	public boolean isValid() throws UnsupportedChecksumAlgorithm {
		long checksum = checksum(array, 0, 8);
		if (checksum == getChecksum()) {
			return true;
		} else {
			return false;
		}
	}

	private long getChecksum() {
		return (((long) array[8] << 56) + ((long) (array[9] & 255) << 48) + ((long) (array[10] & 255) << 40) + ((long) (array[11] & 255) << 32)
				+ ((long) (array[12] & 255) << 24) + ((array[13] & 255) << 16) + ((array[14] & 255) << 8) + ((array[15] & 255) << 0));
	}

	public long getInstanceId() {
		if (instanceId == null) {
			instanceId = (((long) array[0] << 56) + ((long) (array[1] & 255) << 48) + ((long) (array[2] & 255) << 40) + ((long) (array[3] & 255) << 32)
					+ ((long) (array[4] & 255) << 24) + ((array[5] & 255) << 16) + ((array[6] & 255) << 8) + ((array[7] & 255) << 0));
		}
		return instanceId;
	}

	public void writeToStream(RecordFileOut out, Callable<Object> callable) throws IOException {
		out.write(this.array, 0, 16, callable,true);
	}

	private long checksum(byte[] bytes, int offset, int length) throws UnsupportedChecksumAlgorithm {
		Checksum checksum = ChecksumUtil.getCheckSumAlgorithm(ChecksumUtil.PURE_JAVA_CRC32);
		checksum.update(bytes, offset, length);
		return checksum.getValue();
	}

	public static void main(String[] args) throws UnsupportedChecksumAlgorithm {
		ExecuteRecord record = new ExecuteRecord(123199234);
		record.getBytes()[0] = 12;
		ExecuteRecord test = new ExecuteRecord(record.getBytes());
		System.out.println(test.isValid());
		System.out.println(test.getInstanceId());
	}
}
