package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.config.ConfigurationError;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;

public class RwsBufferedRecordFileOutFactory implements RecordFileOutFactory {
	
	protected FlushThreadGroup flushThreadGroup;
	
	@Override
	public void init(Configuration conf) {
		int fileIoBuffsize;
		int fileIoQueueDeep;
		if(conf.getConfigValue("rwsbuffered-file-io-buffsize") == null){
			throw new ConfigurationError("rwsbuffered-file-io-buffsize  not be set, please check your configuration!");
		}else{
			try {
				fileIoBuffsize = Integer.parseInt(conf.getConfigValue("rwsbuffered-file-io-buffsize"));
			} catch (NumberFormatException e) {
				throw new ConfigurationError("rwsbuffered-file-io-buffsize must be number, please check your configuration!");
			}
		}
		if(conf.getConfigValue("rwsbuffered-file-io-queue-deep") == null){
			throw new ConfigurationError("rwsbuffered-file-io-queue-deep  not be set, please check your configuration!");
		}else{
			try {
				fileIoQueueDeep = Integer.parseInt(conf.getConfigValue("rwsbuffered-file-io-queue-deep"));
			} catch (NumberFormatException e) {
				throw new ConfigurationError("rwsbuffered-file-io-queue-deep must be number, please check your configuration!");
			}
		}
		flushThreadGroup = new FlushThreadGroup(fileIoBuffsize, fileIoQueueDeep, "recordwrite", conf.isDebugLog());
	}

	@Override
	public RecordFileOut makeRecordFileOut(File f, long pos) throws IOException {
		return makeRwsRecordFileOut(f, pos);
	}
	
	public RecordFileOut makeRwsRecordFileOut(File f,long pos) throws IOException{
		RandomAccessFile raff = new RandomAccessFile(f, "rws");
		raff.seek(pos);
		return new BufferedFileOut(f.getName(),raff,flushThreadGroup);
	}
	
	public RecordFileOut makeRwRecordFileOut(File f,long pos) throws IOException{
		RandomAccessFile raff = new RandomAccessFile(f, "rw");
		raff.seek(pos);
		return new BufferedFileOut(f.getName(),raff,flushThreadGroup);
	} 

}
