package cn.com.sparkle.firefly.event.listeners;

import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.state.NodeState;

public interface NodeStateChangeEventListener extends EventListener {
	public void loseConnect(NetNode nNode);

	public void openConnect(NetNode nNode);

	public void nodeStateChange(String fromNetNodeAddress, NodeState oldState , NodeState newState);
}
