package cn.com.sparkle.paxos;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.config.ConfigNode;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.state.NodeState;

public class NodesCollection {
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(NodesCollection.class);
	
	private Set<ConfigNode> nodeMembers;
	private Map<String, NetNode> allActiveNodes;
	private Map<String, NetNode> validActiveNodes;
	private Map<String, NodeState> nodeStates;

	public NodesCollection(Set<ConfigNode> nodeMembers, Map<String, NetNode> validActiveNodes, Map<String, NetNode> allActiveNodes,
			Map<String, NodeState> nodeStates) {
		super();
		this.nodeMembers = nodeMembers;
		this.allActiveNodes = allActiveNodes;
		this.validActiveNodes = validActiveNodes;
		this.nodeStates = nodeStates;
	}

	public Set<ConfigNode> getNodeMembers() {
		return nodeMembers;
	}

	public Map<String, NodeState> getNodeStates() {
		return nodeStates;
	}

	public Map<String, NetNode> getAllActiveNodes() {
		return allActiveNodes;
	}

	public Map<String, NetNode> getValidActiveNodes() {
		return validActiveNodes;
	}

}
