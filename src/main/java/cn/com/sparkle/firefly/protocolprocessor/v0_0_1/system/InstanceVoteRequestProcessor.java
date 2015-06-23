package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceVoteRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.ChainVoteCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.votevaluetransport.ValueTransportPipeState;

public class InstanceVoteRequestProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(InstanceVoteRequestProcessor.class);

	private Configuration conf;
	private Context context;

	public InstanceVoteRequestProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.context = context;
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasInstanceVoteRequest()) {
			InstanceVoteRequest request = messagePackage.getInstanceVoteRequest();
			if (logger.isDebugEnabled()) {
				logger.debug("vote instanceId:" + request.getInstanceId());
			}
			ChainVoteCallBackV0_0_1 chainCallback = new ChainVoteCallBackV0_0_1(request.getInstanceId(), request.getChainCount() == 0 ? 1 : 2,
					messagePackage.getId(), session, this);

			SystemNetNode node = null;
			long sendMessageId = -1;
			if (request.getChainCount() != 0) {
				//transport to next node
				LinkedList<String> chain = new LinkedList<String>();
				chain.addAll(request.getChainList()); // protobuf return a immutable list,so we need to rebuild a linkedList
				String next = chain.remove(0);
				node = (SystemNetNode) context.getcState().getSenators().getAllActiveNodes().get(next);
				if (node == null) {
					chainCallback.call(Constants.PAXOS_FAIL_TIME_OUT, null);
				} else {
					Id id = new Id(request.getVoteId().getAddress(), request.getVoteId().getIncreaseId());
					sendMessageId = node.sendInstanceVoteRequest(request.getInstanceId(), id, request.getValuetype(), request.getValueLength(), chain);
				}
			}
			ValueTransportConfig vtc = new ValueTransportConfig(request, node, sendMessageId, chainCallback);
			ValueTransportPipeState valueState = session.get(PaxosSessionKeys.VOTE_VAlUE_TRANSPORT_PIPE_STATE);
			if (valueState == null) {
				valueState = new ValueTransportPipeState();
				session.put(PaxosSessionKeys.VOTE_VAlUE_TRANSPORT_PIPE_STATE, valueState);
			}
			valueState.register(messagePackage.getId(), ValueType.getValueType(request.getValuetype()), request.getValueLength(), vtc);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("start vote value transport pipe: valueLength:%s messageId:%s tomessageId:%s", request.getValueLength(),
						messagePackage.getId(), vtc.getMessageId()));
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	public final static class ValueTransportConfig {
		private InstanceVoteRequest request;
		private SystemNetNode node;
		private long messageId;
		private ChainVoteCallBackV0_0_1 callback;

		public ValueTransportConfig(InstanceVoteRequest request, SystemNetNode node, long messageId, ChainVoteCallBackV0_0_1 callback) {
			super();
			this.request = request;
			this.node = node;
			this.messageId = messageId;
			this.callback = callback;
		}

		public ChainVoteCallBackV0_0_1 getCallback() {
			return callback;
		}

		public InstanceVoteRequest getRequest() {
			return request;
		}

		public SystemNetNode getNode() {
			return node;
		}

		public long getMessageId() {
			return messageId;
		}

	}

}
