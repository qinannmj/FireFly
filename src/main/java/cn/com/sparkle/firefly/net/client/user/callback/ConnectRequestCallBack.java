package cn.com.sparkle.firefly.net.client.user.callback;

import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.ConnectConfig;

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
