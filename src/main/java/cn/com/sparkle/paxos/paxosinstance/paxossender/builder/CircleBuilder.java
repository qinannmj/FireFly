package cn.com.sparkle.paxos.paxosinstance.paxossender.builder;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.NodesCollection;
import cn.com.sparkle.paxos.paxosinstance.paxossender.AbstractInstancePaxosMessageSender;
import cn.com.sparkle.paxos.paxosinstance.paxossender.FullCircleInstancePaxosMessageSender;
import cn.com.sparkle.paxos.paxosinstance.paxossender.HalfCircleInstancePaxosMessageSender;
import cn.com.sparkle.paxos.paxosinstance.paxossender.InstancePaxosMessageSenderBuilderFactory;
import cn.com.sparkle.paxos.route.RouteManage.LinkedNodeList;
import cn.com.sparkle.paxos.util.QuorumCalcUtil;

/**
 * while deepList is not enough to build circle ring, the sender will be transform to boardcast
 * 
 * @author qinan.qn
 *
 */
public class CircleBuilder implements SenderBuilder {
	private LinkedNodeList cacheDeepestLink;
	private AbstractInstancePaxosMessageSender sender;
	private ValidNodeUnChangeBuilder builder = new ValidNodeUnChangeBuilder();

	@Override
	public AbstractInstancePaxosMessageSender buildSender(Context context, String type) {
		NodesCollection nodesCollection = context.getcState().getSenators();
		int quorum = QuorumCalcUtil.calcQuorumNum(nodesCollection.getNodeMembers().size(), context.getConfiguration().getDiskMemLost());
		LinkedNodeList deepestLink = context.getcState().getRouteManage().lookupValidedVoteLink();

		if (deepestLink == cacheDeepestLink) {
			return sender;
		} else {

			if (deepestLink.getList().size() >= quorum) {
				if (InstancePaxosMessageSenderBuilderFactory.HALF_CIRCLE.equals(type)) {
					sender = new HalfCircleInstancePaxosMessageSender(context, deepestLink, quorum);
				} else {
					sender = new FullCircleInstancePaxosMessageSender(context, deepestLink, quorum);
				}
			} else {
				sender = builder.buildSender(context, InstancePaxosMessageSenderBuilderFactory.BOARDCAST);
			}
			cacheDeepestLink = deepestLink;
			return sender;
		}
	}

}
