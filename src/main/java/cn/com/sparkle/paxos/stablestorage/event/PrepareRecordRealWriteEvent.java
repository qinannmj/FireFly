package cn.com.sparkle.paxos.stablestorage.event;

import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

public interface PrepareRecordRealWriteEvent {

	public void successWrite(long instanceId, InstanceVoteRecord record);

	/**
	 * has been writed.
	 * @param instanceId
	 * @param record
	 */
	public void instanceSucceeded(long instanceId, SuccessfulRecord record);

	/**
	 * this callback indicate there is an other master existed in the cluster
	 * @param instanceId
	 */
	public void instanceExecuted(long instanceId);
}
