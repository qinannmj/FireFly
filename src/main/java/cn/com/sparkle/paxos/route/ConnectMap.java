package cn.com.sparkle.paxos.route;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cn.com.sparkle.paxos.config.ConfigNode;

public class ConnectMap {
	private HashMap<String, Vertex> map = new HashMap<String, Vertex>();
	private HashSet<String> allNodeSet = new HashSet<String>();

	public ConnectMap(Set<ConfigNode> newSenators, ConnectMap oldMap) {
		for (ConfigNode node : newSenators) {
			allNodeSet.add(node.getAddress());
		}
		for (String address : allNodeSet) {
			Vertex newV = new Vertex(address, new HashMap<String, Boolean>());
			Vertex oldV = oldMap == null ? null : oldMap.map.get(address);
			for (String add : allNodeSet) {
				if (oldV != null && oldV.getConnectState().containsKey(add)) {
					newV.getConnectState().put(add, oldV.getConnectState().get(add)); //copy and fix state from old
				} else {
					newV.getConnectState().put(add, false);
				}
			}
			map.put(address, newV);
		}
	}

	/**
	 * 
	 * @param e
	 * @param isConnected
	 * @return if is modified
	 */
	public boolean modifyState(Edge e, boolean isConnected) {
		Vertex orgV = map.get(e.getOrg());
		boolean orgIsConnected = orgV.getConnectState().get(e.getDest());
		orgV.getConnectState().put(e.getDest(), isConnected);
		boolean result = orgIsConnected != isConnected;
		if (result) {
			//modify dest state
			Vertex destV = map.get(e.getDest());
			destV.getConnectState().put(e.getOrg(), isConnected);
		}
		return result;
	}

	public HashMap<String, Vertex> getMap() {
		return map;
	}

	public HashSet<String> getAllNodeSet() {
		return allNodeSet;
	}

	public final static class Vertex {
		private HashMap<String, Boolean> connectState;
		private String address;
		private boolean visited = false;

		public Vertex(String address, HashMap<String, Boolean> connectState) {
			super();
			this.address = address;
			this.connectState = connectState;
		}

		public HashMap<String, Boolean> getConnectState() {
			return connectState;
		}

		public String getAddress() {
			return address;
		}

		public boolean isVisited() {
			return visited;
		}

		public void setVisited(boolean visited) {
			this.visited = visited;
		}
	}

	public final static class Edge {
		private String org;
		private String dest;
		private int hashCode;

		public Edge(String org, String dest) {
			if (org.compareTo(dest) > 0) {
				hashCode = org.hashCode();
				this.org = org;
				this.dest = dest;
			} else {
				hashCode = dest.hashCode();
				this.org = dest;
				this.dest = org;
			}
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Edge) {
				Edge e = (Edge) obj;
				return org.equals(e.getOrg()) && dest.equals(e.getDest());
			} else {
				return false;
			}
		}

		public String getOrg() {
			return org;
		}

		public String getDest() {
			return dest;
		}

	}
}
