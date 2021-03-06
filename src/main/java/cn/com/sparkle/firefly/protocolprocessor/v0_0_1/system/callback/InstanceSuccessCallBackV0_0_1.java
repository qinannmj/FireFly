package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.callback.InstanceSucccessCallBack;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class InstanceSuccessCallBackV0_0_1 implements CallBack<MessagePackage> {
	private InstanceSucccessCallBack callback;

	public InstanceSuccessCallBackV0_0_1(InstanceSucccessCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		callback.receivedResponse();
	}

	@Override
	public void fail(NetNode nnode) {
	}
}
