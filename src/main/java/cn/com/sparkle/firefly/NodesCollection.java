package cn.com.sparkle.firefly;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.state.NodeState;

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
	public boolean isSameRoom(String ads1,String ads2){
		NodeState state1 = nodeStates.get(ads1);
		NodeState state2 = nodeStates.get(ads2);
		if(state1 == null || state2 == null){
			return false;
		}else{
			return state1.getRoom().equals(state2.getRoom());
		}
	}
	
	public boolean isArbitrator(String ads){
		NodeState state1 = nodeStates.get(ads);
		if(state1 == null){
			return true;
		}else{
			return state1.isArbitrator();
		}
	}
}
