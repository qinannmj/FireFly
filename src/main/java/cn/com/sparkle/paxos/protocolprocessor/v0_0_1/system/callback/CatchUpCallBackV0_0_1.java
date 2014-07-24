package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRecord;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.CatchUpResponse;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class CatchUpCallBackV0_0_1 implements CallBack<MessagePackage> {
	CatchUpCallBack callback;

	public CatchUpCallBackV0_0_1(CatchUpCallBack callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void call(NetNode nnode, MessagePackage value) {
		CatchUpResponse response = value.getCatchUpResponse();
		if (response.getSuccessfulRecordsCount() != 0) {
			for (CatchUpRecord r : response.getSuccessfulRecordsList()) {
				callback.callback(r.getInstanceId(), r.getValue());
			}
		}
		if (value.getIsLast()) {
			callback.finish();
		}
	}

	@Override
	public void fail(NetNode nnode) {
		callback.fail();
	}

}
