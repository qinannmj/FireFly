package cn.com.sparkle.firefly.net.client.system.callback;

import java.util.concurrent.locks.ReentrantLock;

public abstract class LookUpLatestInstanceIdCallBack extends QuorumCallBack<Object> {

	private long maxInstanceId = -2;

	private ReentrantLock lock = new ReentrantLock();

	public LookUpLatestInstanceIdCallBack(int quorum, int needResponseCount, boolean debugLog) {
		super(quorum, needResponseCount, debugLog);
	}

	public void callback(long instanceId) {
		try {
			lock.lock();
			if (maxInstanceId < instanceId) {
				maxInstanceId = instanceId;
			}
			good();
		} finally {
			lock.unlock();
		}
	}

	public abstract void finish(long instanceId);

	public long getMaxInstanceId() {
		try {
			lock.lock();
			return maxInstanceId;
		} finally {
			lock.unlock();
		}

	}

	@Override
	public void failure(long error, Object errorCause) {
		finish(maxInstanceId);
	}

	@Override
	public void success() {
		finish(maxInstanceId);
	}

	public void fail() {
		try {
			lock.lock();
			netBad();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public String toString() {
		return "LookupLastestInstanceIdCallBack:";
	}

}
