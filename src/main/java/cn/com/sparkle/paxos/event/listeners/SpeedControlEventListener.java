package cn.com.sparkle.paxos.event.listeners;

public interface SpeedControlEventListener extends EventListener {
	public abstract void suggestMaxPackageSize(int suggestSize);
}
