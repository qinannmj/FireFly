package cn.com.sparkle.firefly.paxosinstance;

import java.util.Map;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.paxosinstance.paxossender.ElectionPaxosMessageSender;
import cn.com.sparkle.firefly.state.ClusterState;

public class ElectionPaxosInstance extends PaxosInstance {
	private final static Logger logger = Logger.getLogger(ElectionPaxosInstance.class);

	private final static Value value = new Value(ValueType.COMM, 0);
	private ClusterState cState;
	private ElectionId id;

	public ElectionPaxosInstance(Map<String, NetNode> voteNode, int quorum, ElectionId id, Context context, String address) {
		super(new ElectionPaxosMessageSender(context, voteNode, quorum), 0, address);
		this.cState = context.getcState();
		this.id = new ElectionId(id.getAddress(), id.getIncreaseId(), id.getVersion()); //avoid the propertis of id are changed
	}

	@Override
	public Value getWantAssginValue() {
		return value;
	}

	@Override
	public void voteSuccess(Value value) {
		cState.changeLastElectionId((ElectionId) id);
	}

	@Override
	public void instanceFail(long refuseId, Value value) {
		if (logger.isDebugEnabled()) {
			logger.debug("election refuseId " + refuseId);
		}
		if (refuseId >= cState.getSelfState().getElectionVoteIdBySelf().getIncreaseId()) {
			cState.getSelfState().getElectionVoteIdBySelf().setIncreaseId(refuseId + 1);
		}

	}

	@Override
	public Id getId() {
		return id;
	}

	public Future<Boolean> activate() {
		return super.activate(true);
	}

	@Override
	public Future<Boolean> activate(boolean isWithPreparePhase) {
		throw new RuntimeException("election paxos is not support this method, please invoke activate()");
	}
}
