package cn.com.sparkle.firefly.stablestorage;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.event.events.AccountBookEvent;
import cn.com.sparkle.firefly.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Id;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;
import cn.com.sparkle.firefly.util.IdComparator;

public class AccountBook {
	private final static Logger logger = Logger.getLogger(AccountBook.class);

	private final ReentrantLock writeLock = new ReentrantLock();
	private ExecuteLogOperator executeLogOperator;
	private RecordFileOperator fileOperator;
	private Long lastWaitExecuteInstanceId;
	private Context context;
	private boolean isInitAndNoDamage = false;
	private volatile long instanceIdNoRedoHint;

	public AccountBook(Context context) throws IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm {
		this(context, null);
	}

	public AccountBook(Context context, Long lastExceptInstanceId) throws IOException, UnsupportedChecksumAlgorithm {

		this.context = context;
		lastWaitExecuteInstanceId = lastExceptInstanceId;

	}

	public void init(InstanceExecutor instanceExecutor) throws ClassNotFoundException, IOException, UnsupportedChecksumAlgorithm, InstantiationException,
			IllegalAccessException {
		Configuration configuration = context.getConfiguration();
		String dir = configuration.getStableStorage() + "/" + configuration.getSelfAddress().replaceAll(":", "-");
		FileUtil.getDir(dir);
		File executelogDir = FileUtil.getDir(dir + "/executelog");

		//init executeLog
		executeLogOperator = new ExecuteLogOperator(executelogDir);
		if (lastWaitExecuteInstanceId == null) {
			lastWaitExecuteInstanceId = executeLogOperator.init();
		}
		logger.info("last waited execute instanceId:" + lastWaitExecuteInstanceId);

		fileOperator = StoreVersion.loadRecordFileOperator(dir, lastWaitExecuteInstanceId, instanceExecutor, context);
		
	}
	
	public void initLoad() throws ClassNotFoundException, IOException, UnsupportedChecksumAlgorithm{
		fileOperator.loadData();
		if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
			isInitAndNoDamage = true;
			AccountBookEvent.doInitedEvent(context.getEventsManager());
			if(context.getConfiguration().isDebugLog()){
				logger.debug("init load success!");
			}
		}else{
			if(context.getConfiguration().isDebugLog()){
				logger.debug("init load failed!");
			}
		}
		
	}
	
	public void finishCurInstance(final long instanceId) throws IOException, UnsupportedChecksumAlgorithm {
		instanceIdNoRedoHint = instanceId;
	}

	public boolean writeSuccessfulRecord(final long instanceId, final SuccessfulRecord.Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages)
			throws IOException, UnsupportedChecksumAlgorithm {
		return writeSuccessfulRecord(instanceId, successfulRecord, addRequestPackages,null);
	}
	
	public boolean  writeSuccessfulRecord(final long instanceId, final SuccessfulRecord.Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages,Callable<Object> realEvent) throws IOException, UnsupportedChecksumAlgorithm{
		boolean res = fileOperator.writeSuccessfulRecord(instanceId, successfulRecord, addRequestPackages, realEvent);
		if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
			synchronized (this) {
				if (!fileOperator.isDamaged() && !isInitAndNoDamage) {
					isInitAndNoDamage = true;
					AccountBookEvent.doInitedEvent(context.getEventsManager());
				}
			}
		}
		return res;
	}
	public long getInstanceIdNoRedoHint() {
		return instanceIdNoRedoHint;
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

	public long getKnowedMaxInstanceId() {
		try {
			writeLock.lock();
			if (fileOperator == null) {
				return -1;
			} else {
				return fileOperator.getKnowedMaxId();
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

	public void writeExecuteLog(long instanceId) throws IOException, UnsupportedChecksumAlgorithm {
		executeLogOperator.writeExecuteLog(instanceId);
	}
}
