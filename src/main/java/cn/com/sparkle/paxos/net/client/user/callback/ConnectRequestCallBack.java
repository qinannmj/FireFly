package cn.com.sparkle.paxos.net.client.user.callback;

import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.user.ConnectConfig;

public class ConnectRequestCallBack {
	private ConnectConfig config;
	private NetNode node;

	public void responseGood() {
		config.handed();
	}

	public void responseBad() {
		node.close();
	}

	public ConnectRequestCallBack(ConnectConfig config, NetNode node) {
		super();
		this.config = config;
		this.node = node;
	}

}
