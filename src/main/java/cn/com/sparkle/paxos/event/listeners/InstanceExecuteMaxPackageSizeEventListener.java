package cn.com.sparkle.paxos.event.listeners;

public interface InstanceExecuteMaxPackageSizeEventListener extends EventListener {
	public abstract void maxPackageSizeChange(int curMaxPackageSize);
}
