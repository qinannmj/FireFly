package cn.com.sparkle.firefly.paxosinstance.paxossender;

import java.util.Iterator;
import java.util.LinkedList;

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

/**
 * The full circle mode will degenerate to half circle mode if the num of nodes that's 
 * connectedValidNodesNum more than quorum less than quorum.
 * @author qinan.qn
 *
 */
public class FullCircleInstancePaxosMessageSender extends AbstractInstancePaxosMessageSender {
	private final static Logger logger = Logger.getLogger(FullCircleInstancePaxosMessageSender.class);
	private LinkedNodeList voteLink;

	private LinkedList<SuccessTransportConfig> successList;
	private SystemNetNode successMessageHeadNode;
	private SuccessTransportConfig successMessageHeadTransportConfig;
	private String linkInfo;

	@SuppressWarnings("unchecked")
	public FullCircleInstancePaxosMessageSender(Context context, LinkedNodeList voteLink, int quorum) {
		super(quorum);
		this.voteLink = voteLink;
		LinkedList<SuccessTransportConfig> list = new LinkedList<SuccessTransportConfig>();
		String lastNode = null;
		for (String address : voteLink.getRemainedNodes()) {
			if (address.equals(context.getConfiguration().getSelfAddress())) { //avoid send success message to self
				continue;
			}
			if (lastNode != null) {
				list.addFirst(new SuccessTransportConfig(lastNode, null, true));
			}
			lastNode = address;
		}
		String last = lastNode;
		LinkedList<String> notifyList = new LinkedList<String>();
		notifyList.addAll(voteLink.getRemainedNodes());
		Iterator<String> iter = voteLink.getList().descendingIterator();
		while (iter.hasNext()) {
			String address = iter.next();
			if (lastNode != null) {
				list.addFirst(new SuccessTransportConfig(lastNode, null, lastNode == last));
			}
			lastNode = address;
		}
		//calc success head node
		if (voteLink.getHeadNode().getAddress().equals(context.getConfiguration().getSelfAddress())) {
			if (list.size() > 0) {
				successMessageHeadNode = (SystemNetNode) context.getcState().getSenators().getValidActiveNodes().get(list.getFirst().getTargetAddress());
				successList = (LinkedList<SuccessTransportConfig>) list.clone();
				successMessageHeadTransportConfig = successList.removeFirst();
			}
		} else {
			successMessageHeadNode = voteLink.getHeadNode();
			successList = list;
		}

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("voteNodes:[ ");
		stringBuffer.append(voteLink.getHeadNode().getAddress());
		for (SuccessTransportConfig transport : list) {
			if (transport.isNotifyTransValue()) {
				stringBuffer.append("->");
			} else {
				stringBuffer.append("-->");
			}
			stringBuffer.append(transport.getTargetAddress());
		}
		stringBuffer.append("]");
		linkInfo = stringBuffer.toString();
		if (logger.isDebugEnabled()) {
			logger.debug(linkInfo);
		}
	}

	@Override
	public void sendSuccessRequest(long instanceId, Id id, Value value) {
		//find first 		
		try {
			if (successMessageHeadNode != null) {
				//the head is self, so send the next head
				successMessageHeadNode.sendInstanceSuccessMessage(instanceId, id, successMessageHeadTransportConfig.isNotifyTransValue() ? value : null,
						successMessageHeadTransportConfig.getTargetNotifyAddress(), successList);
			}
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
