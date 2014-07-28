package cn.com.sparkle.firefly.net.client.system.callback;

import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;

public interface PrepareCallBack {
	public void callBad(long lastPrepareId, Value value);

	public void callGood(Id lastId, Value value);

	public void fail();

	@Override
	public String toString();
}
