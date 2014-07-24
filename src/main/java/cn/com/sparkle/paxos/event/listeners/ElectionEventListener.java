package cn.com.sparkle.paxos.event.listeners;

public interface ElectionEventListener extends EventListener {
	public void reElection();
}
