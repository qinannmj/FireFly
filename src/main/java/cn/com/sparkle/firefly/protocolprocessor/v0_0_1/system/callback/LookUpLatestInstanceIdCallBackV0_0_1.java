package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.LookUpLatestInstanceIdResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class LookUpLatestInstanceIdCallBackV0_0_1 implements CallBack<MessagePackage> {
	private LookUpLatestInstanceIdCallBack callback;

	public LookUpLatestInstanceIdCallBackV0_0_1(LookUpLatestInstanceIdCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		LookUpLatestInstanceIdResponse response = value.getLookUpLatestInstanceIdResponse();
		callback.callback(response.getInstanceId());
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();
	}

}
