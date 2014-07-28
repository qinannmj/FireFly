package cn.com.sparkle.firefly.event.listeners;

public interface CatchUpEventListener extends EventListener {

	public abstract void catchUpFail();

	public abstract void recoveryFromFail();
}
