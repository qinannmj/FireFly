package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.model.Value.ValueType;
import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseBad;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseGood;
import cn.com.sparkle.paxos.stablestorage.util.ValueTranslator;

public class PrepareCallBackV0_0_1 implements CallBack<MessagePackage> {
	private PrepareCallBack callback;

	public PrepareCallBackV0_0_1(PrepareCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		if (value.hasPrepareResponseBad()) {
			PrepareResponseBad response = value.getPrepareResponseBad();
			callback.callBad(response.getLastPrepareId(), response.hasSuccessValue() ? ValueTranslator.toValue(response.getSuccessValue()) : null);
		} else {
			PrepareResponseGood response = value.getPrepareResponseGood();
			Id id = new Id(response.getLastVotedId().getAddress(), response.getLastVotedId().getIncreaseId());
			Value v = null;
			if (response.hasValue()) {
				byte[][] byteArray = new byte[response.getValue().getValuesCount()][];
				for (int i = 0; i < byteArray.length; ++i) {
					byteArray[i] = response.getValue().getValues(0).toByteArray();
				}
				v = new Value(ValueType.getValueType(response.getValue().getType()), byteArray);
			}
			callback.callGood(id, v);
		}
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();
	}

}
