package cn.com.sparkle.paxos.stablestorage.upgrade;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.stablestorage.FileDamageException;
import cn.com.sparkle.paxos.stablestorage.ReadRecordCallback;
import cn.com.sparkle.paxos.stablestorage.RecordFileOperator;
import cn.com.sparkle.paxos.stablestorage.SortedReadCallback;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

import com.google.protobuf.GeneratedMessage.Builder;

public class VersionUpgradeTool {
	private final static Logger logger = Logger.getLogger(VersionUpgradeTool.class);

	public boolean update(RecordFileOperator source, RecordFileOperator target) throws IOException, UnsupportedChecksumAlgorithm {
		try {
			SplitReader splitReader = new SplitReader(target, 0);
			long lastExpectSafeInstanceId = source.getLastExpectSafeInstanceId();
			source.readRecord(0, lastExpectSafeInstanceId, splitReader);
			if (splitReader.isError()) {
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
				} catch (Throwable e) {
					logger.error("upgrade error", e);
					isError = true;
				}
			}
		}

		public boolean isError() {
			return isError && successReader.isError;
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
