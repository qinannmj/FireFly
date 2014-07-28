package cn.com.sparkle.firefly.event.listeners;

import cn.com.sparkle.firefly.paxosinstance.InstancePaxosInstance;

public interface ExecutingInstanceChangeEventListener extends EventListener {
	public void entryExecution(InstancePaxosInstance instance);

	public void exitExecution(InstancePaxosInstance instance);
}
