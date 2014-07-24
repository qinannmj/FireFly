package cn.com.sparkle.paxos.stablestorage;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Constants;
import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.addprocess.AddRequestPackage;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.deamon.InstanceExecutor;
import cn.com.sparkle.paxos.event.events.AccountBookEvent;
import cn.com.sparkle.paxos.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.Id;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.paxos.util.IdComparator;

public class AccountBook {
	private final static Logger logger = Logger.getLogger(AccountBook.class);

	private final ReentrantLock writeLock = new ReentrantLock();
	private ExecuteLogOperator executeLogOperator;
	private RecordFileOperator fileOperator;
	private long lastWaitExecuteInstanceId;
	private Context context;
	private boolean isInitAndNoDamage = false;

	public AccountBook(Context context) throws IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm {
		this(context, null);
	}

	public AccountBook(Context context, Long lastExceptInstanceId) throws IOException, UnsupportedChecksumAlgorithm {

		this.context = context;
		Configuration configuration = context.getConfiguration();
		String dir = configuration.getStableStorage() + "/" + configuration.getSelfAddress().replaceAll(":", "-");
		File file = new File(dir);
		if (!file.exists()) {
			file.mkdirs();
		}
		File executelogDir = new File(dir + "/executelog");

		executeLogOperator = new ExecuteLogOperator(executelogDir);
		lastWaitExecuteInstanceId = executeLogOperator.init();
		if (lastExceptInstanceId != null) {
			lastWaitExecuteInstanceId = lastExceptInstanceId;
		}
		logger.info("last waited execute instanceId:" + lastWaitExecuteInstanceId);
	}

	public void init(InstanceExecutor instanceExecutor) throws ClassNotFoundException, IOException, UnsupportedChecksumAlgorithm {
		Configuration configuration = context.getConfiguration();
		String dir = configuration.getStableStorage() + "/" + configuration.getSelfAddress().replaceAll(":", "-");
		fileOperator = StoreVersion.loadRecordFileOperator(dir, lastWaitExecuteInstanceId, instanceExecutor, configuration);
		//		fileOperator = new RecordFileOperator(logDir, lastWaitExecuteInstanceId, instanceExecutor, configuration.getFileChecksumType(),
		//				configuration.isDebugLog());
		fileOperator.loadData();
		if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
			isInitAndNoDamage = true;
			AccountBookEvent.doInitedEvent(context.getEventsManager());
		}
	}

	public void finishCurInstance(final long instanceId) throws IOException, UnsupportedChecksumAlgorithm {
		executeLogOperator.writeExecuteLog(instanceId, null);
	}

	public void writeSuccessfulRecord(final long instanceId, final SuccessfulRecord.Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages)
			throws IOException, UnsupportedChecksumAlgorithm {
		fileOperator.writeSuccessfulRecord(instanceId, successfulRecord, addRequestPackages, null);
		if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
			synchronized (this) {
				if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
					isInitAndNoDamage = true;
					AccountBookEvent.doInitedEvent(context.getEventsManager());
				}
			}
		}
	}

	public boolean isSuccessful(long instanceId) {
		return fileOperator.isSuccessful(instanceId);
	}

	/**
	 * 
	 * @param prepareRecord
	 * @param realWriteEvent
	 * @return Constants.FILE_WRITE_SUCCESS is success,other is increaseId conflicted. 
	 * @throws IOException
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public long writePrepareRecord(long instanceId, Id highestJoinNum, PrepareRecordRealWriteEvent realWriteEvent) throws IOException,
			UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();
			InstanceVoteRecord.Builder prepareRecordBuilder = InstanceVoteRecord.newBuilder().setHighestJoinNum(highestJoinNum);
			InstanceVoteRecord temp = fileOperator.getVotedInstanceRecordMap().get(instanceId);
			if (temp != null) {
				if (IdComparator.getInstance().compare(temp.getHighestJoinNum(), prepareRecordBuilder.getHighestJoinNum()) >= 0) { // conflict
					return temp.getHighestJoinNum().getIncreaseId();
				} else {
					if (temp.hasHighestVotedNum()) { // tranform to vote record
						prepareRecordBuilder.setHighestValue(temp.getHighestValue());
						prepareRecordBuilder.setHighestVotedNum(temp.getHighestVotedNum());
					}

				}
			}
			InstanceVoteRecord r = prepareRecordBuilder.build();
			if (fileOperator.writeVoteRecord(instanceId, r, realWriteEvent)) {
				return Constants.FILE_WRITE_SUCCESS;
			} else {
				return Constants.PAXOS_FAIL_FILE_DAMAGED;
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * 
	 * @param instanceId
	 * @param highestVotedNum
	 * @param highestValue
	 * @param realWriteEvent
	 * @return long Constants.FILE_WRITE_SUCCESS is success,other is increaseId conflicted.
	 * @throws IOException
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public long writeVotedRecord(long instanceId, Id highestVotedNum, Value highestValue, PrepareRecordRealWriteEvent realWriteEvent) throws IOException,
			UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();
			if (isSuccessful(instanceId)) {
				return Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED;
			}
			InstanceVoteRecord record = InstanceVoteRecord.newBuilder().setHighestJoinNum(highestVotedNum).setHighestVotedNum(highestVotedNum)
					.setHighestValue(highestValue).build();
			InstanceVoteRecord temp = fileOperator.getVotedInstanceRecordMap().get(instanceId);
			//when without prepare phase , the temp is null
			if (temp == null || IdComparator.getInstance().compare(record.getHighestVotedNum(), temp.getHighestJoinNum()) == 0) {
				if (fileOperator.writeVoteRecord(instanceId, record, realWriteEvent)) {
					return Constants.FILE_WRITE_SUCCESS;
				} else {
					return Constants.PAXOS_FAIL_FILE_DAMAGED;
				}
			}
			return temp.getHighestJoinNum().getIncreaseId();
		} finally {
			writeLock.unlock();
		}
	}

	public long getMaxInstanceIdInVote() {
		try {
			writeLock.lock();
			if (fileOperator == null) {
				return -1;
			} else {
				return fileOperator.getMaxVoteInstanceId();
			}
		} finally {
			writeLock.unlock();
		}
	}

	public long getLastCanExecutableInstanceId() {
		if (fileOperator == null) {
			return -1;
		} else {
			return fileOperator.getLastExpectSafeInstanceId() - 1;
		}
	}

	public void readSuccessRecord(long fromInstanceId, long toInstanceId, ReadSuccessRecordCallback successCallback) throws IOException,
			UnsupportedChecksumAlgorithm {
		try {
			fileOperator.readRecord(fromInstanceId, toInstanceId, successCallback);
		} catch (FileDamageException e) {
			logger.warn("file damage", e);
		}
	}

	/**
	 * 
	 * @return -1 indicates there is not remainder in queue
	 */
	public long getFisrtInstnaceIdOfWaitInsertToExecuteQueue() {
		if (fileOperator == null) {
			return -1;
		} else {
			return fileOperator.getFirstInstanceIdInUnsafe();
		}
	}
}
