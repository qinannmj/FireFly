package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.VoteResponse;
import cn.com.sparkle.paxos.stablestorage.util.ValueTranslator;

public class VoteCallBackV0_0_1 implements CallBack<MessagePackage> {
	private VoteCallBack callback;

	public VoteCallBackV0_0_1(VoteCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		VoteResponse response = value.getVoteResponse();
		callback.call(response.getRefuseId(), response.hasSuccessValue() ? ValueTranslator.toValue(response.getSuccessValue()) : null);
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();
	}

}
