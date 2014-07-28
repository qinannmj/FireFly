package cn.com.sparkle.firefly.net.client;

public interface CallBack<T> {
	public void call(NetNode nnode, T value);

	public void fail(NetNode nnode);
}
