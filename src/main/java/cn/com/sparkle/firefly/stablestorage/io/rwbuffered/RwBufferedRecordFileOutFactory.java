package cn.com.sparkle.firefly.stablestorage.io.rwbuffered;

import java.io.File;
import java.io.IOException;

import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.RwsBufferedRecordFileOutFactory;

public class RwBufferedRecordFileOutFactory extends RwsBufferedRecordFileOutFactory {
	@Override
	public RecordFileOut makeRecordFileOut(File f, long pos) throws IOException {
		return makeRwRecordFileOut(f, pos);
	}

}
