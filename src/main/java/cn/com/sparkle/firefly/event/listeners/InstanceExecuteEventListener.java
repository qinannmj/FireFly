package cn.com.sparkle.firefly.event.listeners;

import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;

public interface InstanceExecuteEventListener extends EventListener {
	public abstract void instanceExecuted(SuccessfulRecord record);
}
