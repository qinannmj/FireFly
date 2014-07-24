package cn.com.sparkle.paxos.net.client.system.callback;

import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.paxosinstance.PaxosInstance;

public class PaxosPrepareCallBack extends QuorumCallBack<Value> implements PrepareCallBack {
	private PaxosInstance paxosInstance;
	private ReentrantLock lock = new ReentrantLock();
	private volatile Value lastValue = null;
	private volatile Id lastVotedId = new Id("", -1);

	public PaxosPrepareCallBack(int quorum, int needResponseCount, PaxosInstance paxosInstance) {
		this(quorum, needResponseCount, paxosInstance, paxosInstance.debugLog);
	}

	public PaxosPrepareCallBack(int quorum, int needResponseCount, PaxosInstance paxosInstance, boolean isDebug) {
		super(quorum, needResponseCount, isDebug);
		this.paxosInstance = paxosInstance;
	}

	@Override
	public void success() {
		paxosInstance.prepareSuccess(lastValue);
	}

	@Override
	public void failure(long error, Value errorCause) {
		paxosInstance.fail(error, errorCause);
	}

	@Override
	public void callGood(Id lastId, Value value) {
		try {
			lock.lock();
			if (lastVotedId.compareTo(lastId) == -1 && value != null) {
				// if (lastVotedId.compareTo(response.lastVotedId) == -1) {
				lastVotedId = lastId;
				lastValue = value;
			}
			super.good();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void callBad(long lastPrepareId, Value value) {
		try {
			lock.lock();
			super.bad(lastPrepareId, value);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void fail() {
		try {
			lock.lock();
			super.netBad();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public String toString() {
		return "PaxosPrepareCallBack:" + paxosInstance.getInstanceId();
	}

	public Value getLastValue() {
		return lastValue;
	}

	public Id getLastVotedId() {
		return lastVotedId;
	}

}
