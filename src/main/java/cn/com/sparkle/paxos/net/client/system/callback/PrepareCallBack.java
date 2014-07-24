package cn.com.sparkle.paxos.net.client.system.callback;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;

public interface PrepareCallBack {
	public void callBad(long lastPrepareId, Value value);

	public void callGood(Id lastId, Value value);

	public void fail();

	@Override
	public String toString();
}
