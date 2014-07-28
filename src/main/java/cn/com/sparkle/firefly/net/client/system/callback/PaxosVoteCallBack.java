package cn.com.sparkle.firefly.net.client.system.callback;

import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.paxosinstance.PaxosInstance;

public class PaxosVoteCallBack extends QuorumCallBack<Value> implements VoteCallBack {

	private PaxosInstance paxosInstance;

	private ReentrantLock lock = new ReentrantLock();

	public PaxosVoteCallBack(int quorum, int needResponseCount, PaxosInstance paxosInstance) {
		super(quorum, needResponseCount, paxosInstance.debugLog);
		this.paxosInstance = paxosInstance;
	}

	@Override
	public void call(long _refuseId, Value value) {
		try {
			lock.lock();
			if (_refuseId == Constants.VOTE_OK) {
				good();
			} else {
				bad(_refuseId, value);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void fail() {
		try {
			lock.lock();
			netBad();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void success() {
		paxosInstance.voteSuccess();
	}

	@Override
	public void failure(long error, Value errorCause) {
		paxosInstance.fail(error, errorCause);
	}

	@Override
	public String toString() {
		return "VoteCallBack:" + paxosInstance.getInstanceId();
	}
}
