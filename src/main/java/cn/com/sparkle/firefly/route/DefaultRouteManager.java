package cn.com.sparkle.firefly.route;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.ConfigureEventListener;
import cn.com.sparkle.firefly.event.listeners.MasterDistanceChangeListener;
import cn.com.sparkle.firefly.event.listeners.NodeStateChangeEventListener;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.route.ConnectMap.Edge;
import cn.com.sparkle.firefly.route.ConnectMap.Vertex;
import cn.com.sparkle.firefly.state.ClusterState;
import cn.com.sparkle.firefly.state.NodeState;
import cn.com.sparkle.firefly.util.QuorumCalcUtil;

public class DefaultRouteManager implements RouteManage, MasterDistanceChangeListener, NodeStateChangeEventListener,
		ConfigureEventListener {
	//	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(DefaultRouteManager.class);

	private Configuration conf;
	private ClusterState clusterState;
	private int curDistance = Integer.MAX_VALUE;
	private volatile int cacheDistance = Integer.MIN_VALUE;

	private volatile String upLevelAddress = null;
	private volatile SystemNetNode upLevelNetNode = null;
	private volatile LinkedNodeList linkedNodeList = new LinkedNodeList(null, new LinkedList<String>(), new HashSet<ConnectMap.Edge>(), new HashSet<String>());
	private volatile ConnectMap connectMap;
	private volatile HashMap<String, RouteState> routeMap = new HashMap<String, RouteState>();

	public DefaultRouteManager(Configuration conf, ClusterState clusterState, EventsManager eventsManager) {
		this.conf = conf;
		this.clusterState = clusterState;
		connectMap = new ConnectMap(conf.getConfigNodeSet().getSenators(), null);
		routeMap = new HashMap<String, RouteState>(conf.getConfigNodeSet().getSenators().size());
		for (ConfigNode n : conf.getConfigNodeSet().getSenators()) {
			routeMap.put(n.getAddress(), new RouteState(null));
		}
		eventsManager.registerListener(this);
	}

	@Override
	public SystemNetNode lookupRouteNode(String address) {
		RouteState routeState = routeMap.get(address);
		return routeState == null ? null : (SystemNetNode) routeState.node;
	}

	@Override
	public LinkedNodeList lookupValidedVoteLink() {
		return linkedNodeList;
	}

	@Override
	public String lookupUpLevelNodeAddress() {
		return upLevelAddress;
	}

	@Override
	public String lookupUpLevelNodeAddress(int distance) {
		if (cacheDistance != distance) {
			NodesCollection senator = clusterState.getSenators();
			logger.info("look senator " + senator);
			upLevelAddress = null;
			upLevelNetNode = null;
			for (NetNode n : senator.getAllActiveNodes().values()) {
				NodeState ns = senator.getNodeStates().get(n.getAddress());
				if (!conf.getSelfAddress().equals(n.getAddress()) && ns.getMasterDistance() + 1 == distance) { //except self
					upLevelAddress = n.getAddress();
					upLevelNetNode = (SystemNetNode) n;
					break;
				}
			}
			cacheDistance = distance;
		}
		return upLevelAddress;
	}

	@Override
	public void masterDistanceChange(int distance) {
		lookupUpLevelNodeAddress(distance);//active modify uplevelNode address
		if (distance == 0) {
			//this is master
			curDistance = distance;
			recalc();
		}
	}

	@Override
	public SystemNetNode lookupUpLevelNode() {
		return upLevelNetNode;
	}

	@Override
	public void loseConnect(NetNode nNode) {
	}

	@Override
	public void openConnect(NetNode nNode) {
	}

	@Override
	public void senatorsChange(Set<ConfigNode> newSenators, ConfigNode addNode, ConfigNode rmNode, long version) {
		ConnectMap newConnectMap = new ConnectMap(newSenators, connectMap);//re create new ConnectMap
		HashMap<String, RouteState> newMap = new HashMap<String, RouteState>();
		for (ConfigNode n : newSenators) {//copy route state
			RouteState rs = routeMap.get(n.getAddress());
			newMap.put(n.getAddress(), rs != null ? rs : new RouteState(null));
		}

		//check key path is modify
		connectMap = newConnectMap;
		routeMap = newMap;
		recalc();

	}

	private void recalc() {
		//In a cyclic,directed graph finding longest path program is npc.Because i estimate the scale of node is not big, normally low than and equal 7, i 
		//search the quorum path by dfs (allow repeated visit node that is visited in previous rounds),
		//i can estimate complexity is o(n!/quorum!).
		//The good point that use inapproximate algorithm in this process is the system can find a quorum path to guarantee the system can run in worst situation.
		//Maybe can use a approximate algorithm that have a good approximate ratio to improve this process in future.
		if (curDistance == 0) {
			int quorum = QuorumCalcUtil.calcQuorumNum(conf.getConfigNodeSet().getSenators().size(), conf.getDiskMemLost());
			//prior to select self
			String master = conf.getSelfAddress();
			Vertex startV = connectMap.getMap().get(master);
			DfsCalcList dfsCalcList = new DfsCalcList();
			if (startV != null) {
				dfs(dfsCalcList, quorum, startV); // search master is enough to search the linkedlist
			} else {
				for (Vertex v : connectMap.getMap().values()) {
					dfs(dfsCalcList, quorum, v);//search the every branch
				}
			}
			HashSet<String> remainedNodes = new HashSet<String>();
			remainedNodes.addAll(connectMap.getAllNodeSet());
			NodesCollection collection = clusterState.getSenators(); // get connect snapshot
			SystemNetNode head = null;

			if (dfsCalcList.result.size() != 0 && collection.getValidActiveNodes().containsKey(dfsCalcList.result.getFirst())) {
				for (String node : dfsCalcList.result) {
					remainedNodes.remove(node);
				}
				head = (SystemNetNode) collection.getValidActiveNodes().get(dfsCalcList.result.getFirst());
			} else {
				dfsCalcList.result.clear();
			}
			LinkedNodeList linkedNodeList = new LinkedNodeList(head, dfsCalcList.result, dfsCalcList.resultEdge, remainedNodes);
			this.linkedNodeList = linkedNodeList;
		}
	}

	/*
	 * return if find k-path
	 * search the branch with biggest weight
	 */
	@SuppressWarnings("all")
	private void dfs(DfsCalcList dfsCalcList, int k, Vertex v) {

		v.setVisited(true);
		String lnode = dfsCalcList.temp.size() == 0 ? null : dfsCalcList.temp.getLast();
		dfsCalcList.temp.addLast(v.getAddress());//push to temp
		try {
			if (lnode == null || clusterState.getSenators().isSameRoom(v.getAddress(), lnode)) {
				dfsCalcList.tempWeight += 1;
			} else {
				dfsCalcList.tempWeight -= 1;
			}
			if (k == 1) {
				if (dfsCalcList.tempWeight > dfsCalcList.resultWeight) {
					dfsCalcList.result = (LinkedList<String>) dfsCalcList.temp.clone();
					dfsCalcList.resultEdge = (HashSet<Edge>) dfsCalcList.tempEdge.clone();
					dfsCalcList.resultWeight = dfsCalcList.tempWeight;
				}
				return;
			}
			for (Entry<String, Boolean> e : v.getConnectState().entrySet()) {
				if (e.getValue()) {
					Vertex nextV = connectMap.getMap().get(e.getKey());
					if (!nextV.isVisited()) {
						Edge edge = new Edge(v.getAddress(), nextV.getAddress());
						dfsCalcList.tempEdge.add(edge);
						dfs(dfsCalcList, k - 1, nextV);
						dfsCalcList.tempEdge.remove(edge);
					}
				}
			}
		} finally {
			dfsCalcList.temp.removeLast();//pop from temp
			v.setVisited(false);
		}
	}

	private final static class DfsCalcList {
		private LinkedList<String> temp = new LinkedList<String>();
		private LinkedList<String> result = new LinkedList<String>();
		private HashSet<Edge> tempEdge = new HashSet<ConnectMap.Edge>();
		private HashSet<Edge> resultEdge = new HashSet<ConnectMap.Edge>();
		private int resultWeight = Integer.MIN_VALUE;
		private int tempWeight = 0;
	}

	private final static class RouteState {
		private volatile NetNode node;

		public RouteState(NetNode node) {
			this.node = node;
		}
	}

	@Override
	public void nodeStateChange(String fromAddress, NodeState oldState, NodeState newState) {
		RouteState rs = routeMap.get(newState.getAddress());
		if (rs != null) {
			//this rs == null maybe happened in the version of senator be different to remote's.

			rs.node = clusterState.getSenators().getAllActiveNodes().get(fromAddress); //modify route state
		}
		ConnectMap cacheConnectMap = connectMap; //variable in function, avoid concurrently modify referance
		boolean kPathModify = false;
		boolean connectModify = false;
		HashSet<String> connectNode = new HashSet<String>();
		for (String dest : newState.getConnectedValidNode()) {//process connected node map
			if (conf.getConfigNodeSet().getSenatorsMap().containsKey(dest) && conf.getConfigNodeSet().getSenatorsMap().containsKey(newState.getAddress())) {
				Edge e = new Edge(newState.getAddress(), dest);
				boolean isModify = cacheConnectMap.modifyState(e, true);
				if (isModify) {
					connectModify = true;
				}
				connectNode.add(dest);
			}
		}
		connectModify = connectModify || oldState == null || newState == null || (!oldState.getRoom().equals(newState.getRoom()));
		for (String address : cacheConnectMap.getAllNodeSet()) {//process disconnected node map
			if (!connectNode.contains(address) && conf.getConfigNodeSet().getSenatorsMap().containsKey(address)
					&& conf.getConfigNodeSet().getSenatorsMap().containsKey(newState.getAddress())) {
				Edge e = new Edge(newState.getAddress(), address);
				boolean isModify = cacheConnectMap.modifyState(e, false);
				if (isModify) {
					kPathModify = linkedNodeList.relatedEdges.contains(e); // if the key path is modified
				}
			}
		}

		if (kPathModify || connectModify) {
			recalc();
		}
	}

}
