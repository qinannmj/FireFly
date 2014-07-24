package cn.com.sparkle.paxos.event.listeners;

import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.state.NodeState;

public interface NodeStateChangeEventListener extends EventListener {
	public void loseConnect(NetNode nNode);

	public void openConnect(NetNode nNode);

	public void beatHeart(NetNode nNode, NodeState nState);

	public void activeBeatHeart(String fromAddress, NodeState nState);
}
