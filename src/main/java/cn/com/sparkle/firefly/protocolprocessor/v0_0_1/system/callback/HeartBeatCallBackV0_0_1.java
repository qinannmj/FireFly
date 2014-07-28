package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.SenatorHeartBeatResponse;

public class HeartBeatCallBackV0_0_1 implements CallBack<MessagePackage> {

	private cn.com.sparkle.firefly.net.client.system.callback.HeartBeatCallBack callback;

	public HeartBeatCallBackV0_0_1(cn.com.sparkle.firefly.net.client.system.callback.HeartBeatCallBack callback) {
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		SenatorHeartBeatResponse response = value.getSenatorHeartBeatResponse();
		callback.call(nnode, response.getIsMasterConnected(), response.getElectionId(), response.getElectionAddress(), response.getElectionVersion(),
				response.getLastCanExecuteInstanceId(), response.getIsInited(), response.getIsUpToDate(), response.getMasterDistance(),
				response.getConnectedValidNodesList());
	}

	@Override
	public void fail(NetNode nnode) {
	}
}
