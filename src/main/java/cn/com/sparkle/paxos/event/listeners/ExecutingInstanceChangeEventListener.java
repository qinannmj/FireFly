package cn.com.sparkle.paxos.event.listeners;

import cn.com.sparkle.paxos.paxosinstance.InstancePaxosInstance;

public interface ExecutingInstanceChangeEventListener extends EventListener {
	public void entryExecution(InstancePaxosInstance instance);

	public void exitExecution(InstancePaxosInstance instance);
}
