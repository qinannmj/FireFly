package cn.com.sparkle.paxos.stablestorage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import com.google.protobuf.GeneratedMessage.Builder;

import cn.com.sparkle.paxos.addprocess.AddRequestPackage;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.deamon.InstanceExecutor;
import cn.com.sparkle.paxos.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

public interface RecordFileOperator {

	/**
	 * just for init operator
	 */
	public void initOperator(File dir, long lastExpectSafeInstanceId, InstanceExecutor instanceExecutor, Configuration conf);

	public long loadData() throws IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm;

	public boolean writeSuccessfulRecord(long instanceId, SuccessfulRecord.Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages,
			final Callable<Object> realEvent) throws IOException, UnsupportedChecksumAlgorithm;

	/**
	 * 
	 * @param instanceId
	 * @param highestJoinNum
	 * @param highestVotedNum
	 * @param highestValue
	 * @param realWriteEvent
	 * @return is can write
	 * @throws IOException
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public boolean writeVoteRecord(final long instanceId, final InstanceVoteRecord record, final PrepareRecordRealWriteEvent realWriteEvent)
			throws IOException, UnsupportedChecksumAlgorithm;

	public long getMinSuccessRecordInstanceId();

	public long getLastExpectSafeInstanceId();

	public HashMap<Long, InstanceVoteRecord> getVotedInstanceRecordMap();

	public long getMaxVoteInstanceId();

	public boolean isSuccessful(long instanceId);

	/**
	 * 
	 * @param fromInstanceId >=
	 * @param toInstanceId <=
	 * @param successCallback
	 * @throws IOException
	 * @throws UnsupportedChecksumAlgorithm
	 */
	@SuppressWarnings("rawtypes")
	public void readRecord(long fromInstanceId, long toInstanceId, ReadRecordCallback<Builder<? extends Builder>> readCallback) throws IOException, UnsupportedChecksumAlgorithm,
			FileDamageException;

	public long getFirstInstanceIdInUnsafe();

	public boolean isDamaged();

}
