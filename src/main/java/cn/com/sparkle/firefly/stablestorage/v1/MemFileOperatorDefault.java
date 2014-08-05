package cn.com.sparkle.firefly.stablestorage.v1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.config.ConfigurationError;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord.Builder;

public class MemFileOperatorDefault extends RecordFileOperatorDefault{
	private final static Logger logger = Logger.getLogger(MemFileOperatorDefault.class);
	
	private ArrayList<Builder> successList;
	
	@Override
	public void initOperator(File dir, long lastExpectSafeInstanceId, InstanceExecutor instanceExecutor, RecordFileOutFactory recordOutFactory,
			Configuration conf) {
		super.initOperator(dir, lastExpectSafeInstanceId, instanceExecutor, new RecordFileOutFactory() {
			
			@Override
			public RecordFileOut makeRecordFileOut(File f, long pos) throws IOException {
				return new RecordFileOut() {
					
					@Override
					public long writeLong(long v, Callable<Object> callable) throws IOException {
						exec(callable);
						return 0;
					}
					
					@Override
					public long writeInt(int size, Callable<Object> callable) throws IOException {
						exec(callable);
						return 0;
					}
					
					@Override
					public long write(byte[] buf, int off, int length, Callable<Object> callable) throws IOException {
						exec(callable);
						return 0;
					}
					
					@Override
					public void close() throws IOException {
					}
					private void exec(Callable<Object> call){
						if(call != null){
							try {
								call.call();
							} catch (Exception e) {
								logger.error("unexcepted error",e);
							}
						}
					}
				};
			}
			
			@Override
			public void init(Configuration conf) throws ConfigurationError {
			}
		}, conf);
	}
	
	@Override
	public boolean writeSuccessfulRecord(long instanceId, Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages, Callable<Object> realEvent)
			throws IOException, UnsupportedChecksumAlgorithm {
		boolean result = super.writeSuccessfulRecord(instanceId, successfulRecord, addRequestPackages, realEvent);
		if(result == true){
			
		}
		return result;
	}
}
