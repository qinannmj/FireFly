package cn.com.sparkle.firefly.net.client.user;

import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.net.client.user.callback.ConnectRequestCallBack;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;

public class UserNetNode extends NetNode {

	public UserNetNode(PaxosSession session, String address, Protocol protocol, String appVersion, int heartBeatInterval) {
		super(session, address, protocol, appVersion, heartBeatInterval);
	}

	public void sendConnectRequest(int masterDistance, ConnectRequestCallBack callback) {
		long packageId = this.generatePackageId();
		byte[][] request = getProtocol().createConnectRequsetRequest(packageId, masterDistance);
		CallBack<? extends Object> _callback = getProtocol().createConnectRequestCallBack(callback);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendAddRequest(CommandType commandType,long instanceId, byte[] value, AddRequestCallBack callback) throws NetCloseException {
		long packageId = this.generatePackageId();
		byte[][] request = getProtocol().createAddRequest(packageId, commandType, value,instanceId);
		CallBack<? extends Object> _callback = getProtocol().createAddRequestCallBack(callback);
		this.write(request, packageId, _callback);
	}

	public void sendHeartBeat(AddRequestCallBack callback) throws NetCloseException {
		long packageId = this.generatePackageId();
		byte[][] request = getProtocol().createHeartBeatRequest(packageId);
		CallBack<? extends Object> _callback = getProtocol().createAddRequestCallBack(callback);
		this.write(request, packageId, _callback);
	}
	@Override
	protected void onClose() {
		super.onClose();
	}
}
