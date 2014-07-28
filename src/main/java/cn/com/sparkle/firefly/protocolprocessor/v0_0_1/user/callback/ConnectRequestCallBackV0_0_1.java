package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.callback;

import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.callback.ConnectRequestCallBack;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class ConnectRequestCallBackV0_0_1 implements CallBack<MessagePackage> {
	ConnectRequestCallBack callback;

	public ConnectRequestCallBackV0_0_1(ConnectRequestCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		if (value.getConnectResponse().getIsSuccessful()) {
			callback.responseGood();
		} else {
			callback.responseBad();
		}
	}

	@Override
	public void fail(NetNode nnode) {
		callback.responseBad();
	}

}
