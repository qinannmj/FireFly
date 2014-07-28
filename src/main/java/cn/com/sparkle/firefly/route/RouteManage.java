package cn.com.sparkle.firefly.route;

import java.util.HashSet;
import java.util.LinkedList;

import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.route.ConnectMap.Edge;

public interface RouteManage {
	public SystemNetNode lookupRouteNode(String address);

	public String lookupUpLevelNodeAddress();

	public SystemNetNode lookupUpLevelNode();

	public String lookupUpLevelNodeAddress(int distance);

	public LinkedNodeList lookupValidedVoteLink();

	public final static class LinkedNodeList {
		private SystemNetNode headNode;
		protected LinkedList<String> list = null;
		protected LinkedList<String> noHeadList = null;
		protected HashSet<Edge> relatedEdges = new HashSet<ConnectMap.Edge>();
		private HashSet<String> remainedNodes;

		@SuppressWarnings("unchecked")
		public LinkedNodeList(SystemNetNode node, LinkedList<String> list, HashSet<Edge> relatedEdges, HashSet<String> remainedNodes) {
			super();
			this.headNode = node;
			this.list = list;
			this.relatedEdges = relatedEdges;
			this.remainedNodes = remainedNodes;
			noHeadList = (LinkedList<String>) list.clone();
			if (noHeadList.size() != 0) {
				noHeadList.removeFirst();
			}
		}

		public SystemNetNode getHeadNode() {
			return headNode;
		}

		public LinkedList<String> getList() {
			return list;
		}

		public HashSet<String> getRemainedNodes() {
			return remainedNodes;
		}

		public LinkedList<String> getNoHeadList() {
			return noHeadList;
		}

		public HashSet<Edge> getRelatedEdges() {
			return relatedEdges;
		}

	}
}
