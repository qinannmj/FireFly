package cn.com.sparkle.firefly.paxosinstance.paxossender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.client.system.callback.PaxosPrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PaxosVoteCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.firefly.paxosinstance.PaxosInstance;

public class BoardcastInstancePaxosMessageSender extends AbstractInstancePaxosMessageSender {

	private final static Logger logger = Logger.getLogger(BoardcastInstancePaxosMessageSender.class);

	private HashSet<NetNode> voteSet;

	private Map<String, List<String>> notifyShip = new HashMap<String, List<String>>();

	private Configuration conf;

	private String linkInfo = "";

	public BoardcastInstancePaxosMessageSender(NodesCollection nodesCollection, int quorum, Configuration conf) {
		super(quorum);
		this.conf = conf;
		voteSet = new HashSet<NetNode>(quorum);
		Map<String, NetNode> validActiveNode = nodesCollection.getValidActiveNodes();
		LinkedList<NetNode> notifySet = new LinkedList<NetNode>();
		for (NetNode node : validActiveNode.values()) {
			if (quorum != 0) {
				voteSet.add(node);
				--quorum;
			} else if (node.getAddress().equals(conf.getSelfAddress())) {
				// prior to use self
				for (NetNode n : voteSet) {
					voteSet.remove(n);
					notifySet.add(n);
					voteSet.add(node);
					break;
				}
			} else {
				notifySet.add(node);
			}
		}
		//calc notify ship
		int scalePerNode = (int) Math.ceil((double) notifySet.size() / (voteSet.size() - 1));//ignore one node joined vote.
		for (NetNode node : voteSet) {
			if (node.getAddress().equals(conf.getSelfAddress())) {
				continue; // ignore self
			}
			List<String> list = new LinkedList<String>();
			for (int i = 0; i < scalePerNode; ++i) {
				if (notifySet.size() != 0) {
					NetNode n = notifySet.remove();
					list.add(n.getAddress());
				}
			}
			notifyShip.put(node.getAddress(), list);
		}

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("voteNodes:[ ");
		for (NetNode node : voteSet) {
			stringBuffer.append(node.getAddress()).append("(notifyShip:");
			List<String> list = notifyShip.get(node.getAddress());
			if (list != null) {
				for (String notifyAddress : notifyShip.get(node.getAddress())) {
					stringBuffer.append(notifyAddress).append(",");
				}
			}
			stringBuffer.append(") ");
		}
		stringBuffer.append("]");
		linkInfo = stringBuffer.toString();
		if (conf.isDebugLog()) {
			logger.debug(linkInfo);
		}
	}

	@Override
	public void sendPrepareRequest(PaxosInstance paxosInstance, long instanceId, Id id) {
		PrepareCallBack callback = new PaxosPrepareCallBack(quorum, voteSet.size(), paxosInstance);
		for (NetNode node : voteSet) {
			((SystemNetNode) node).sendInstancePrepareRequest(id, instanceId, null, callback);
		}
	}

	@Override
	public void sendVoteRequest(PaxosInstance paxosInstance, long instanceId, Id id, Value value) {
		VoteCallBack callback = new PaxosVoteCallBack(quorum, voteSet.size(), paxosInstance);
		for (NetNode node : voteSet) {
			((SystemNetNode) node).sendInstanceVoteRequest(instanceId, id, value, null, callback);
		}
	}

	@Override
	public void sendSuccessRequest(long instanceId, Id id, Value value) {
		for (NetNode node : voteSet) {
			if (!node.getAddress().equals(conf.getSelfAddress())) {
				try {
					((SystemNetNode) node).sendInstanceSuccessMessage(instanceId, id, null, notifyShip.get(node.getAddress()), null);
				} catch (InterruptedException e) {
					logger.error("unexcepted error", e);
				}
			}
		}
	}

	@Override
	public String linkInfo() {
		return linkInfo;
	}
}
