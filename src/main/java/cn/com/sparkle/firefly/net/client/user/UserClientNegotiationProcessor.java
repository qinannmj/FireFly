package cn.com.sparkle.firefly.net.client.user;

import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.callback.ConnectRequestCallBack;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.AbstractClientNegotiationProcessor;

public class UserClientNegotiationProcessor extends AbstractClientNegotiationProcessor {

	public UserClientNegotiationProcessor(int preferChecksumType, int heartBeatInterval, ProtocolManager protocolManager) {
		super(preferChecksumType, heartBeatInterval, protocolManager);
	}

	@Override
	public NetNode createNetNode(String appVersion, String address, PaxosSession session, Protocol protocol, int heartBeatInterval) {
		ConnectConfig config = session.get(PaxosSessionKeys.USER_CLIENT_CONNECT_CONFIG);
		UserNetNode netNode = new UserNetNode(session, config.getAddress(), protocol, appVersion, heartBeatInterval);
		ConnectRequestCallBack callback = new ConnectRequestCallBack(config, netNode);
		netNode.sendConnectRequest(config.getMasterDistance(), callback);
		config.connected(netNode);
		return netNode;
	}

}
