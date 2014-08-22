package cn.com.sparkle.firefly.stablestorage.upgrade;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.FileDamageException;
import cn.com.sparkle.firefly.stablestorage.ReadRecordCallback;
import cn.com.sparkle.firefly.stablestorage.RecordFileOperator;
import cn.com.sparkle.firefly.stablestorage.SortedReadCallback;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;

import com.google.protobuf.GeneratedMessage.Builder;

public class VersionUpgradeTool {
	private final static Logger logger = Logger.getLogger(VersionUpgradeTool.class);

	public boolean update(RecordFileOperator source, RecordFileOperator target) throws IOException, UnsupportedChecksumAlgorithm {
		try {
			SplitReader splitReader = new SplitReader(target, 0);
			source.readRecord(0, Long.MAX_VALUE, splitReader);
			if (splitReader.isError()) {
				logger.warn(String.format("source lastExpectSafeInstanceId %s target lastExpectSafeInstanceId %s", source.getLastExpectSafeInstanceId(),target.getLastExpectSafeInstanceId()));
				return false;
			}
		} catch (IOException e) {
			throw e;
		} catch (UnsupportedChecksumAlgorithm e) {
			throw e;
		} catch (FileDamageException e) {
			//this can be ignored exception
			return false;
		}
		return true;
	}
	@SuppressWarnings("rawtypes")
	private static class SplitReader implements ReadRecordCallback<Builder<? extends Builder>> {
		private RecordFileOperator out;
		private SortedReadCallback<SuccessfulRecord.Builder> sortedReadCallback;
		private SuccessReader successReader;
		private boolean isError = false;

		public SplitReader(RecordFileOperator out, long exceptedNextId) {
			this.out = out;
			successReader = new SuccessReader(out);
			sortedReadCallback = new SortedReadCallback<SuccessfulRecord.Builder>(successReader, exceptedNextId);
		}
		
		@Override
		public void read(long instanceId, Builder<? extends Builder> b) {
			if (b instanceof SuccessfulRecord.Builder) {
				sortedReadCallback.read(instanceId, (SuccessfulRecord.Builder)b);
			} else {
				try {
					out.writeVoteRecord(instanceId, (InstanceVoteRecord) b.build(), null);
				}catch (Throwable e) {
					logger.error("upgrade error", e);
					isError = true;
				}
			}
		}

		public boolean isError() {
			return isError || successReader.isError || !sortedReadCallback.isFinished() ;
		}
	}
	private static class SuccessReader implements ReadRecordCallback<SuccessfulRecord.Builder> {
		private RecordFileOperator out;
		private boolean isError = false;

		public SuccessReader(RecordFileOperator out) {
			this.out = out;
		}
		
		@Override
		public void read(long instanceId, SuccessfulRecord.Builder b) {
			try {
				out.writeSuccessfulRecord(instanceId, b, null, null);
			} catch (Throwable e) {
				logger.error("upgrade error", e);
				isError = true;
			}
		}
	}

}
