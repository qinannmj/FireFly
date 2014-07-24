package cn.com.sparkle.paxos.paxosinstance.paxossender.builder;

import java.util.Map;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.NodesCollection;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.paxosinstance.paxossender.AbstractInstancePaxosMessageSender;
import cn.com.sparkle.paxos.paxosinstance.paxossender.BoardcastInstancePaxosMessageSender;
import cn.com.sparkle.paxos.paxosinstance.paxossender.InstancePaxosMessageSenderBuilderFactory;
import cn.com.sparkle.paxos.util.QuorumCalcUtil;

public class ValidNodeUnChangeBuilder implements SenderBuilder {

	private Map<String, NetNode> validNodeCache = null;
	private AbstractInstancePaxosMessageSender sender;

	@Override
	public AbstractInstancePaxosMessageSender buildSender(Context context, String type) {
		NodesCollection nodesCollection = context.getcState().getSenators();
		if (nodesCollection.getValidActiveNodes() == validNodeCache) {
			return sender;
		} else {
			int quorum = QuorumCalcUtil.calcQuorumNum(nodesCollection.getNodeMembers().size(), context.getConfiguration().getDiskMemLost());
			if (nodesCollection.getValidActiveNodes().size() >= quorum) {
				if (InstancePaxosMessageSenderBuilderFactory.BOARDCAST.equals(type)) {
					sender = new BoardcastInstancePaxosMessageSender(nodesCollection, quorum, context.getConfiguration());
				} else {
					throw new RuntimeException("No match sender,");
				}
			} else {
				return null;
			}
			validNodeCache = nodesCollection.getValidActiveNodes();
			return sender;
		}
	}

}
