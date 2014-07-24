package cn.com.sparkle.paxos.event.listeners;

import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

public interface InstanceExecuteEventListener extends EventListener {
	public abstract void instanceExecuted(SuccessfulRecord record);
}
