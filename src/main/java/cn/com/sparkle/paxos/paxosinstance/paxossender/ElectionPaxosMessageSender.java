package cn.com.sparkle.paxos.paxosinstance.paxossender;

import java.util.Map;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.model.ElectionId;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.system.SystemNetNode;
import cn.com.sparkle.paxos.net.client.system.callback.PaxosPrepareCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.PaxosVoteCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.paxos.paxosinstance.PaxosInstance;

public class ElectionPaxosMessageSender extends AbstractPaxosMessageSender {

	private Map<String, NetNode> voteSet;

	private Context context;

	public ElectionPaxosMessageSender(Context context, Map<String, NetNode> voteSet, int quorum) {
		super(quorum);
		this.voteSet = voteSet;
		this.context = context;
	}

	@Override
	public void sendPrepareRequest(PaxosInstance paxosInstance, long instanceId, Id id) {
		PrepareCallBack callback = new PaxosPrepareCallBack(this.quorum, voteSet.size(), paxosInstance);
		for (NetNode node : voteSet.values()) {
			((SystemNetNode) node).sendElectionPrepareRequest((ElectionId) id, context.getAccountBook().getMaxInstanceIdInVote(), callback);
		}
	}

	@Override
	public void sendVoteRequest(PaxosInstance paxosInstance, long instanceId, Id id, Value value) {
		VoteCallBack callback = new PaxosVoteCallBack(quorum, voteSet.size(), paxosInstance);
		for (NetNode node : voteSet.values()) {
			((SystemNetNode) node).sendElectionVoteRequest((ElectionId) id, context.getAccountBook().getMaxInstanceIdInVote(), callback);
		}
	}

	@Override
	public void sendSuccessRequest(long instanceId, Id id, Value value) {
		for (NetNode node : voteSet.values()) {
			((SystemNetNode) node).sendElectionSuccessMessage((ElectionId) id);
		}
	}

	@Override
	public String linkInfo() {
		return "boarding to all node!";
	}

}
