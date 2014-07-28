package cn.com.sparkle.firefly.event.listeners;

public interface SpeedControlEventListener extends EventListener {
	public abstract void suggestMaxPackageSize(int suggestSize);
}
