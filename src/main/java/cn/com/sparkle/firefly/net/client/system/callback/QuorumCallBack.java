package cn.com.sparkle.firefly.net.client.system.callback;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;

public abstract class QuorumCallBack<T> {
	private final static Logger logger = Logger.getLogger(PrepareCallBack.class);
	private int needResponseCount;
	private final int quorum;

	private int goodNum = 0;
	private int badNum = 0;
	private int netBadNum = 0;
	private long error = Constants.PAXOS_FAIL_TIME_OUT;
	private T errorCause;


	public QuorumCallBack(int quorum, int needResponseCount) {
		this.needResponseCount = needResponseCount;
		this.quorum = quorum;
	}

	protected void netBad() {
		--needResponseCount;
		++netBadNum;
		checkCount();
	}

	protected void bad(long errorCode, T errorCause) {
		if (errorCode > error) {
			error = errorCode;
			this.errorCause = errorCause;
		}
		++badNum;
		--needResponseCount;
		checkCount();
	}

	protected void good() {
		++goodNum;
		--needResponseCount;
		checkCount();
	}

	private void checkCount() {
		if (logger.isDebugEnabled()) {
			logger.debug(toString() + "goodNum:" + goodNum + " badNum:" + badNum + " netBadNum:" + netBadNum + " needResponseCount:" + needResponseCount);
		}
		if (goodNum == quorum) {
			++goodNum;// avoid invoking repeatedly
			success();
		} else if (goodNum < quorum && needResponseCount == 0) {
			if (netBadNum < quorum) {
				failure(error, this.errorCause);
			} else {
				failure(Constants.PAXOS_FAIL_TIME_OUT, null);
			}
		}
	}

	public Object getErrorCause() {
		return errorCause;
	}

	protected abstract void success();

	protected abstract void failure(long error, T errorCause);
}
