package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.callback;

import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.AddResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class AddRequestCallBackV0_0_1 implements CallBack<MessagePackage> {

	private AddRequestCallBack callback;

	public AddRequestCallBackV0_0_1(AddRequestCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		AddResponse response = value.getAddResponse();
		callback.call(response.getResult().toByteArray(), response.hasInstanceId() ? response.getInstanceId() : -1, value.getIsLast());
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();

	}

}
