package cn.com.sparkle.firefly.net.client.system.callback;

import cn.com.sparkle.firefly.model.Value;

public interface VoteCallBack {
	public void call(long _refuseId, Value value);

	public void fail();

	@Override
	public String toString();
}
