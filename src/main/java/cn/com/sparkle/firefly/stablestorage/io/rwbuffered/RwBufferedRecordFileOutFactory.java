package cn.com.sparkle.firefly.stablestorage.io.rwbuffered;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.BufferedFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.RwsBufferedRecordFileOutFactory;

public class RwBufferedRecordFileOutFactory extends RwsBufferedRecordFileOutFactory {
	@Override
	public RecordFileOut makeRecordFileOut(File f, long pos) throws IOException {
		RandomAccessFile raff = new RandomAccessFile(f, "rw");
		raff.seek(pos);
		return new BufferedFileOut(raff,super.flushThreadGroup);
	}

}
