package cn.com.sparkle.paxos.event.listeners;

public interface MasterDistanceChangeListener extends EventListener {
	public void masterDistanceChange(int distance);
}
