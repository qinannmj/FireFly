package cn.com.sparkle.firefly.checksum;

import java.util.zip.Checksum;

public class NoChecksum implements Checksum {
	@Override
	public void update(int b) {
	}

	@Override
	public void update(byte[] b, int off, int len) {
	}

	@Override
	public long getValue() {
		return 0;
	}

	@Override
	public void reset() {
	}
}
