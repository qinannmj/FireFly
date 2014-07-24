package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.user.callback;

import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class AddRequestCallBackV0_0_1 implements CallBack<MessagePackage> {

	private AddRequestCallBack callback;

	public AddRequestCallBackV0_0_1(AddRequestCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		callback.call(value.getAddResponse().getResult().toByteArray(), value.getIsLast());
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();

	}

}
