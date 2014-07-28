package cn.com.sparkle.firefly.event.listeners;

public interface InstanceExecuteMaxPackageSizeEventListener extends EventListener {
	public abstract void maxPackageSizeChange(int curMaxPackageSize);
}
