package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseBad;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseGood;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;

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
				v = ValueTranslator.toValue(response.getValue());
			}
			callback.callGood(id, v);
		}
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();
	}

}
