package cn.com.sparkle.firefly.paxosinstance.paxossender;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.SuccessTransportConfig;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.client.system.callback.PaxosPrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PaxosVoteCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.firefly.paxosinstance.PaxosInstance;
import cn.com.sparkle.firefly.route.RouteManage.LinkedNodeList;

public class HalfCircleInstancePaxosMessageSender extends AbstractInstancePaxosMessageSender {

	private final static Logger logger = Logger.getLogger(HalfCircleInstancePaxosMessageSender.class);
	private LinkedNodeList voteLink;

	private LinkedList<SuccessTransportConfig> notifyShip = new LinkedList<SuccessTransportConfig>();

	private SystemNetNode successMessageHeadNode;
	private SuccessTransportConfig successMessageHeadTransportConfig;

	private String linkInfo;

	public HalfCircleInstancePaxosMessageSender(Context context, LinkedNodeList voteLink, int quorum) {
		super(quorum);
		this.voteLink = voteLink;
		String selfAddress = context.getConfiguration().getSelfAddress();
		//calc notify ship
		LinkedList<String> notifyList = new LinkedList<String>();
		notifyList.addAll(voteLink.getRemainedNodes());
		Iterator<String> iter = voteLink.getList().iterator();
		iter.next(); // skip the leader
		while (iter.hasNext()) {
			// #message transfor path
			//    vote leader
			//         \
			//        voter 1-notify1
			//            \
			//           voter 2-notify2

			String node = iter.next();
			List<String> list = new LinkedList<String>();
			if (notifyList.size() != 0) {
				String address = notifyList.remove();
				if (!address.equals(selfAddress)) {//avoid send success to self
					list.add(address);
				}
			}
			SuccessTransportConfig config = new SuccessTransportConfig(node, list, false);
			notifyShip.addLast(config);
		}

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("voteNodes:[ ");
		stringBuffer.append(voteLink.getList().getFirst() + " ");
		for (SuccessTransportConfig transport : notifyShip) {
			stringBuffer.append(transport.getTargetAddress()).append("(notifyShip:");
			for (String notifyAddress : transport.getTargetNotifyAddress()) {
				stringBuffer.append(notifyAddress).append(",");
			}
			stringBuffer.append(") ");
		}
		stringBuffer.append("]");
		linkInfo = stringBuffer.toString();
		//debug output
		if (context.getConfiguration().isDebugLog()) {
			logger.debug(linkInfo);
		}

		//calc cache for runtime
		String headNode = voteLink.getHeadNode().getAddress();

		if (!headNode.equals(selfAddress)) {
			successMessageHeadTransportConfig = new SuccessTransportConfig(headNode, null, false);
		} else {
			successMessageHeadTransportConfig = notifyShip.removeFirst();
		}
		successMessageHeadNode = (SystemNetNode) context.getcState().getSenators().getValidActiveNodes()
				.get(successMessageHeadTransportConfig.getTargetAddress());

	}

	@Override
	public void sendSuccessRequest(long instanceId, Id id, Value value) {

		try {
			successMessageHeadNode.sendInstanceSuccessMessage(instanceId, id, successMessageHeadTransportConfig.isNotifyTransValue() ? value : null,
					successMessageHeadTransportConfig.getTargetNotifyAddress(), notifyShip);
		} catch (InterruptedException e) {
			//may be lost the headNode, cancel the process ,and wait other node to study the successful instance actively.
		}
	}

	@Override
	public void sendPrepareRequest(PaxosInstance paxosInstance, long instanceId, Id id) {
		PrepareCallBack callback = new PaxosPrepareCallBack(1, 1, paxosInstance);
		voteLink.getHeadNode().sendInstancePrepareRequest(id, instanceId, voteLink.getNoHeadList(), callback);
	}

	@Override
	public void sendVoteRequest(PaxosInstance paxosInstance, long instanceId, Id id, Value value) {
		VoteCallBack callback = new PaxosVoteCallBack(1, 1, paxosInstance);
		voteLink.getHeadNode().sendInstanceVoteRequest(instanceId, id, value, voteLink.getNoHeadList(), callback);
	}

	@Override
	public String linkInfo() {
		return linkInfo;
	}
}
