package cn.com.sparkle.firefly.paxosinstance;

import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.future.SystemFuture;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.paxosinstance.paxossender.PaxosMessageSender;

public abstract class PaxosInstance {
	private final static Logger logger = Logger.getLogger(PaxosInstance.class);
	/**
	 * if conflictId is Constants.PAXOS_FAIL_TIME_OUT,it may indicates that response from node is not
	 * enough half because of timeout.
	 */
	protected long instanceId;

	private Value lastValue;
	private SystemFuture<Boolean> future;
	private volatile PaxosMessageSender sender;

	public PaxosInstance(PaxosMessageSender sender, long instanceId, String selfAddress) {
		super();
		this.instanceId = instanceId;
		this.sender = sender;
	}

	public Future<Boolean> activate(boolean isWithPreparePhase) {
		future = new SystemFuture<Boolean>();
		//for optimize the rt by reduce prepare phase
		if (isWithPreparePhase) {
			sender.sendPrepareRequest(this, instanceId, getId());
		} else {
			lastValue = getWantAssginValue();
			sender.sendVoteRequest(this, instanceId, getId(), lastValue);
		}
		return future;
	}

	public void resetActiveNode(PaxosMessageSender sender) {
		this.sender = sender;
	}

	public void fail(long refuseId, Value value) {
		instanceFail(refuseId, value);
		future.set(false);
	}

	public final void prepareSuccess(Value returnLastValue) {
		if (logger.isDebugEnabled()) {
			logger.debug("prepareSuccessEvent returnValue isNull:" + (returnLastValue == null));
		}
		lastValue = returnLastValue != null ? returnLastValue : getWantAssginValue();
		sender.sendVoteRequest(this, instanceId, getId(), lastValue);
	}

	public final long getInstanceId() {
		return instanceId;
	}

	public final void voteSuccess() {
		if (logger.isDebugEnabled()) {
			logger.debug("voteSuccess instanceId:" + instanceId);
		}
		voteSuccess(lastValue);
		sender.sendSuccessRequest(instanceId, getId(), lastValue);
		future.set(true);
	}

	public abstract Value getWantAssginValue();

	/**
	 * 
	 * @param value
	 * @return transport obj to consume in jvm
	 */
	public abstract void voteSuccess(Value value);

	public abstract Id getId();

	/**
	 * 
	 * @param refuseId
	 *            Constants.PAXOS_FAIL_TIME_OUT indicates the number of active senator node is not enough
	 */
	public abstract void instanceFail(long refuseId, Value value);

}
