package cn.com.sparkle.firefly.event.listeners;

public interface MasterChangePosEventListener extends EventListener {
	public abstract void getMasterPos();

	public abstract void lostPos();

	public abstract void masterChange(String address);
}
