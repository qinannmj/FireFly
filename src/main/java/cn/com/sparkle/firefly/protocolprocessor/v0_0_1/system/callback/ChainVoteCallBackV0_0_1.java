package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.system.callback.QuorumCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.VoteResponse;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;

public class ChainVoteCallBackV0_0_1 extends QuorumCallBack<Value> implements VoteCallBack {

	private long instanceId; //just for trace
	private long messageId;
	private PaxosSession session;
	private AbstractProtocolV0_0_1Processor processor;

	public ChainVoteCallBackV0_0_1(long instanceId, int needResponseCount, long messageId, PaxosSession session, AbstractProtocolV0_0_1Processor processor) {
		super(needResponseCount, needResponseCount);
		this.instanceId = instanceId;
		this.messageId = messageId;
		this.session = session;
		this.processor = processor;
	}

	@Override
	public synchronized void call(long _refuseId, Value value) {
		if (_refuseId == Constants.VOTE_OK) {
			good();
		} else {
			bad(_refuseId, value);
		}
	}

	@Override
	public synchronized void fail() {
		netBad();
	}

	@Override
	protected void success() {
		VoteResponse.Builder response = VoteResponse.newBuilder().setRefuseId(Constants.VOTE_OK);
		MessagePackage.Builder builder = MessagePackage.newBuilder();
		builder.setVoteResponse(response);
		builder.setId(messageId);
		builder.setIsLast(true);
		// send response
		processor.sendResponse(session, builder.build());
	}

	@Override
	protected void failure(long error, Value value) {
		VoteResponse.Builder response = VoteResponse.newBuilder().setRefuseId(error);
		if (value != null) {
			response.setSuccessValue(ValueTranslator.toStoreModelValue(value));
		}
		MessagePackage.Builder responseBuilder = MessagePackage.newBuilder();
		responseBuilder.setVoteResponse(response);
		responseBuilder.setId(messageId);
		responseBuilder.setIsLast(true);
		processor.sendResponse(session, responseBuilder.build());
	}

	@Override
	public String toString() {
		return "chainVote instanceId:" + instanceId;
	}
}
