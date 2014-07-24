package cn.com.sparkle.paxos.event.listeners;

public interface CatchUpEventListener extends EventListener {

	public abstract void catchUpFail();

	public abstract void recoveryFromFail();
}
