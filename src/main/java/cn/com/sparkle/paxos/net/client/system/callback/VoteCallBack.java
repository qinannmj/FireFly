package cn.com.sparkle.paxos.net.client.system.callback;

import cn.com.sparkle.paxos.model.Value;

public interface VoteCallBack {
	public void call(long _refuseId, Value value);

	public void fail();

	@Override
	public String toString();
}
