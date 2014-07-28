package cn.com.sparkle.firefly.state;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.MasterChangePosEvent;
import cn.com.sparkle.firefly.event.events.MasterDistanceChangeEvent;
import cn.com.sparkle.firefly.event.listeners.ConfigureEventListener;
import cn.com.sparkle.firefly.event.listeners.NodeStateChangeEventListener;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.route.DefaultRouteManager;
import cn.com.sparkle.firefly.route.RouteManage;

/**
 * i adopt multi version to enhance the performance in concurrency.So the new instance of
 * NodesCollection may be created a lot of times.
 * 
 * @author nanqi
 * 
 */
public class ClusterState implements ConfigureEventListener, NodeStateChangeEventListener {
	private final static Logger logger = Logger.getLogger(ClusterState.class);

	private Configuration conf;

	// cur master
	private volatile ElectionId lastElectionId;
	private volatile int masterDistance = Constants.MAX_MASTER_INSTANCE;

	private volatile NodesCollection senators;
	//	private volatile NodesCollection followers;

	private EventsManager eventsManager;

	private SelfState selfState;

	private ReentrantLock nodeLock = new ReentrantLock();

	private RouteManage routeManage;

	public ClusterState(EventsManager eventsManager, final Configuration conf) {

		this.conf = conf;
		this.selfState = new SelfState(eventsManager);
		this.eventsManager = eventsManager;
		this.lastElectionId = new ElectionId("", -1, conf.getConfigNodeSet().getVersion());
		HashMap<String, NodeState> temp = new HashMap<String, NodeState>();
		for (ConfigNode node : conf.getConfigNodeSet().getSenators()) {
			String address = node.getAddress();
			NodeState ns = new NodeState(address);
			temp.put(address, ns);
		}
		senators = new NodesCollection(conf.getConfigNodeSet().getSenators(), new HashMap<String, NetNode>(), new HashMap<String, NetNode>(), temp);

		eventsManager.registerListener(this);
		this.routeManage = new DefaultRouteManager(conf, this, eventsManager);

	}

	private ReentrantLock electionIdLock = new ReentrantLock();

	public void changeLastElectionId(ElectionId lastElectionId) {
		try {
			electionIdLock.lock();

			if (lastElectionId.compareTo(this.lastElectionId) != 0) {
				if (conf.isDebugLog()) {
					logger.debug("master change:" + lastElectionId.getIncreaseId() + "  " + lastElectionId.getAddress());
				}
				if (this.lastElectionId.getAddress().equals(conf.getSelfAddress())) {
					if (!lastElectionId.getAddress().equals(conf.getSelfAddress())) {
						setMasterDistance(Constants.MAX_MASTER_INSTANCE);
						MasterChangePosEvent.doLostPosEvent(eventsManager);
					}
				} else if (lastElectionId.getAddress().equals(conf.getSelfAddress())) {
					if (!this.lastElectionId.getAddress().equals(conf.getSelfAddress())) {
						setMasterDistance(0);
						MasterChangePosEvent.doGetMasterPosEvent(eventsManager);
					}
				} else {
					setMasterDistance(Constants.MAX_MASTER_INSTANCE);
				}
				this.lastElectionId = lastElectionId;
				this.selfState.getElectionVoteIdBySelf().setIncreaseId(lastElectionId.getIncreaseId() + 1);
				MasterChangePosEvent.doMasterChangeEvent(eventsManager, lastElectionId.getAddress());
			}
		} finally {
			electionIdLock.unlock();
		}
	}

	public void lostAuthorizationOfMaster() {
		try {
			electionIdLock.lock();
			if (lastElectionId.getAddress().equals(conf.getSelfAddress())) {
				lastElectionId = new ElectionId("", -1, lastElectionId.getVersion());
				if (conf.isDebugLog()) {
					logger.debug("master change:" + lastElectionId.getIncreaseId() + "  " + lastElectionId.getAddress());
				}
				setMasterDistance(Constants.MAX_MASTER_INSTANCE);
				MasterChangePosEvent.doLostPosEvent(eventsManager);
			}
		} finally {
			electionIdLock.unlock();
		}
	}

	private void setMasterDistance(int distance) {
		try {
			nodeLock.lock();
			this.masterDistance = distance;
			MasterDistanceChangeEvent.masterDistanceChange(eventsManager, distance);
		} finally {
			nodeLock.unlock();
		}
	}

	@Override
	public void loseConnect(NetNode nNode) {
		try {
			nodeLock.lock();
			String address = nNode.getAddress();

			if (ConfigNode.exist(senators.getNodeMembers(), address)) {
				HashMap<String, NodeState> newNodeState = new HashMap<String, NodeState>();
				newNodeState.putAll(senators.getNodeStates());
				newNodeState.put(address, new NodeState(address));
				HashMap<String, NetNode> newAllActiveNodes = new HashMap<String, NetNode>();
				newAllActiveNodes.putAll(senators.getAllActiveNodes());
				newAllActiveNodes.remove(nNode.getAddress());

				Map<String, NetNode> newvalidActiveNodes;
				if (senators.getValidActiveNodes().containsKey(address)) {
					newvalidActiveNodes = new HashMap<String, NetNode>();
					newvalidActiveNodes.putAll(senators.getValidActiveNodes());
					newvalidActiveNodes.remove(nNode.getAddress());
				} else {
					newvalidActiveNodes = senators.getValidActiveNodes();
				}

				senators = new NodesCollection(senators.getNodeMembers(), newvalidActiveNodes, newAllActiveNodes, newNodeState);
			}
			//reset distance ,and wait heartbeat to fix the distance
			if (masterDistance != 0) {
				setMasterDistance(Constants.MAX_MASTER_INSTANCE);
			}
			if (conf.isDebugLog()) {
				logger.debug("lost a node " + address);
			}
		} finally {
			nodeLock.unlock();
		}

	}

	@Override
	public void openConnect(NetNode nNode) {
		try {
			nodeLock.lock();
			if (ConfigNode.exist(senators.getNodeMembers(), nNode.getAddress())) {
				HashMap<String, NetNode> newAllActiveNodes = new HashMap<String, NetNode>();
				newAllActiveNodes.putAll(senators.getAllActiveNodes());
				newAllActiveNodes.put(nNode.getAddress(), (SystemNetNode) nNode);

				//				NodeState nodeState = new NodeState(nNode.getAddress());
				//				nodeState.setConnected(true);
				//				HashMap<String, NodeState> newNodeState = new HashMap<String, NodeState>();
				//				newNodeState.putAll(senators.getNodeStates());
				//				newNodeState.put(nNode.getAddress(), nodeState);

				senators = new NodesCollection(senators.getNodeMembers(), senators.getValidActiveNodes(), newAllActiveNodes, senators.getNodeStates());
			}
			if (ClusterState.this.conf.isDebugLog()) {
				logger.debug("add a node " + nNode.getAddress());
			}
		} finally {
			nodeLock.unlock();
		}

	}

	@Override
	public void beatHeart(NetNode nNode, NodeState nState) {
		try {
			nodeLock.lock();

			if (ConfigNode.exist(senators.getNodeMembers(), nState.getAddress())) {
				HashMap<String, NodeState> newNodeState = new HashMap<String, NodeState>();
				newNodeState.putAll(senators.getNodeStates());
				newNodeState.put(nState.getAddress(), nState);

				Map<String, NetNode> initedActiveSenators;
				if (nState.isInit() && !senators.getValidActiveNodes().containsKey(nNode.getAddress()) && nState.isUpToDate()) {
					initedActiveSenators = new HashMap<String, NetNode>();
					initedActiveSenators.putAll(senators.getValidActiveNodes());
					initedActiveSenators.put(nNode.getAddress(), (SystemNetNode) nNode);
				} else if (!nState.isUpToDate() && senators.getValidActiveNodes().containsKey(nNode.getAddress())) {
					initedActiveSenators = new HashMap<String, NetNode>();
					initedActiveSenators.putAll(senators.getValidActiveNodes());
					initedActiveSenators.remove(nNode.getAddress());
				} else {
					initedActiveSenators = senators.getValidActiveNodes();
				}

				senators = new NodesCollection(senators.getNodeMembers(), initedActiveSenators, senators.getAllActiveNodes(), newNodeState);
			} else {
				followerConnect(nState);
			}
			//modify distance
			if (!nState.getAddress().equals(conf.getSelfAddress())) {
				int distance = nState.getMasterDistance() == Constants.MAX_MASTER_INSTANCE ?Constants.MAX_MASTER_INSTANCE :  nState.getMasterDistance() + 1;
				if (masterDistance > distance) {
					setMasterDistance(distance);
				}
			}
		} finally {
			nodeLock.unlock();
		}

	}

	@Override
	public void activeBeatHeart(String fromAddress, NodeState nState) {
		try {
			nodeLock.lock();
			if (!ConfigNode.exist(senators.getNodeMembers(), nState.getAddress())) {
				followerConnect(nState);
			}
		} finally {
			nodeLock.unlock();
		}
	}

	@Override
	public void senatorsChange(Set<ConfigNode> newSenators, ConfigNode addNode, ConfigNode rmNode, long version) {
		try {
			nodeLock.lock();
			if (rmNode != null) {
				rmNode.invalid();
			}

			HashMap<String, NetNode> allActiveNodes = new HashMap<String, NetNode>();
			HashMap<String, NetNode> validActiveNodes = new HashMap<String, NetNode>();

			for (NetNode nNode : senators.getAllActiveNodes().values()) {
				if (ConfigNode.exist(newSenators, nNode.getAddress())) {
					allActiveNodes.put(nNode.getAddress(), nNode);
					if (senators.getValidActiveNodes().containsKey(nNode.getAddress())) {
						validActiveNodes.put(nNode.getAddress(), nNode);
					}
				} else {
					nNode.close();
				}
			}
			HashMap<String, NodeState> temp = new HashMap<String, NodeState>();
			for (ConfigNode node : newSenators) {
				String address = node.getAddress();
				clearFollower(address);

				if (senators.getNodeStates().containsKey(address)) {
					temp.put(address, senators.getNodeStates().get(address));
				} else {
					temp.put(address, new NodeState(address));
				}
			}
			senators = new NodesCollection(newSenators, validActiveNodes, allActiveNodes, temp);
			lostAuthorizationOfMaster();
			selfState.reInit();
			
		} finally {
			nodeLock.unlock();
		}
	}

	//	@Override
	//	public void followersChange(Set<ConfigNode> newFollowers) {
	//		try {
	//			nodeLock.lock();
	//			HashMap<String,NetNode> allActiveNodes = new HashMap<String,NetNode>();
	//			HashMap<String,NetNode> validActiveNodes = new HashMap<String,NetNode>();
	//
	//			for (NetNode nNode : followers.getAllActiveNodes().values()) {
	//				if (newFollowers.contains(ConfigNode.parseNode(nNode.getAddress()))) {
	//					allActiveNodes.put(nNode.getAddress(),nNode);
	//					if (followers.getValidActiveNodes().containsKey(nNode.getAddress())) {
	//						validActiveNodes.put(nNode.getAddress(),nNode);
	//					}
	//				}
	//			}
	//			HashMap<String, NodeState> temp = new HashMap<String, NodeState>();
	//			for (ConfigNode node : newFollowers) {
	//				String address = node.getAddress();
	//				if (followers.getNodeStates().containsKey(address)) {
	//					temp.put(address, followers.getNodeStates().get(address));
	//				} else {
	//					temp.put(address, new NodeState(address));
	//				}
	//			}
	//			followers = new NodesCollection(newFollowers, validActiveNodes, allActiveNodes, temp);
	//		} finally {
	//			nodeLock.unlock();
	//		}
	//
	//	}

	public ElectionId getLastElectionId() {
		return lastElectionId;
	}

	public SelfState getSelfState() {
		return selfState;
	}

	public NodesCollection getSenators() {
		return senators;
	}

	//	public NodesCollection getFollowers() {
	//		return followers;
	//	}

	public int getMasterDistance() {
		return masterDistance;
	}

	public RouteManage getRouteManage() {
		return routeManage;
	}

	public List<NodeState> getFollowers() {
		try {
			nodeLock.lock();
			LinkedList<NodeState> list = new LinkedList<NodeState>();
			for (NodeState ns : followerMap.values()) {
				list.add(ns);
			}
			for (NodeState ns : followerMapLongTime.values()) {
				list.add(ns);
			}
			return list;
		} finally {
			nodeLock.unlock();
		}
	}

	private HashMap<String, NodeState> followerMap = new HashMap<String, NodeState>();
	private HashMap<String, NodeState> followerMapLongTime = new HashMap<String, NodeState>();
	private long followerLastCheckTime;

	private void followerConnect(NodeState nodeState) {
		String address = nodeState.getAddress();
		followerMap.put(address, nodeState);
		followerMapLongTime.remove(address);
		long now = System.currentTimeMillis();
		if (now - 2 * Constants.MAX_HEART_BEAT_INTERVAL > followerLastCheckTime) {
			followerLastCheckTime = now;
			followerMapLongTime.clear();
			HashMap<String, NodeState> temp = followerMapLongTime;
			followerMapLongTime = followerMap;
			followerMap = temp;
		}
	}

	private void clearFollower(String address) {
		followerMap.remove(address);
		followerMapLongTime.remove(address);
	}

}
