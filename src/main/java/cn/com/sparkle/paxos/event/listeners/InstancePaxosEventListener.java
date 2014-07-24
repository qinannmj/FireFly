package cn.com.sparkle.paxos.event.listeners;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.paxosinstance.InstancePaxosInstance;

public interface InstancePaxosEventListener extends EventListener {
	/**
	 * 
	 * @param instance
	 * @param refuseId
	 * @param value succeeded instance's value
	 *            Constants.PAXOS_FAIL_TIME_OUT indicates the number of active senator node is not enough
	 */
	public abstract void instanceFail(InstancePaxosInstance instance, Id id, long refuseId, Value value);

	public abstract void instanceSuccess(InstancePaxosInstance instance, Value value);

	public abstract void instanceStart(InstancePaxosInstance instance);
}
