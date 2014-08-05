package cn.com.sparkle.firefly.stablestorage.io;

import java.io.File;
import java.io.IOException;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.config.ConfigurationError;

public interface RecordFileOutFactory {
	public void init(Configuration conf) throws ConfigurationError;
	public RecordFileOut makeRecordFileOut(File f,long pos) throws IOException;
}
