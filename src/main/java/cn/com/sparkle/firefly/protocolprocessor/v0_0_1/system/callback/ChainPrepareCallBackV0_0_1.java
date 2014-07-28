package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback;

import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.system.callback.PaxosPrepareCallBack;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseBad;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.PrepareResponseGood;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstancePrepareRequestProcessor;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel;
import cn.com.sparkle.firefly.stablestorage.util.IdTranslator;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;

public class ChainPrepareCallBackV0_0_1 extends PaxosPrepareCallBack {
	private InstancePrepareRequestProcessor processor;

	private long messageId;
	private PaxosSession session;
	private long instanceId; //just for trace

	public ChainPrepareCallBackV0_0_1(InstancePrepareRequestProcessor processor, long instanceId, int needResponseCount, long messageId, PaxosSession session,
			boolean isDebug) {
		super(needResponseCount, needResponseCount, null, isDebug);
		this.messageId = messageId;
		this.session = session;
		this.instanceId = instanceId;
		this.processor = processor;
	}

	@Override
	public void success() {
		Value lastValue = getLastValue();
		StoreModel.Id.Builder sid = IdTranslator.toStoreModelId(getLastVotedId());

		PrepareResponseGood.Builder response = PrepareResponseGood.newBuilder().setLastVotedId(sid);
		if (lastValue != null) {
			response.setValue(ValueTranslator.toStoreModelValue(lastValue));
		}
		MessagePackage.Builder builder = MessagePackage.newBuilder();
		builder.setPrepareResponseGood(response);
		builder.setId(messageId);
		builder.setIsLast(true);
		processor.sendResponse(session, builder.build().toByteArray());
	}

	@Override
	public void failure(long error, Value successValue) {
		PrepareResponseBad.Builder response = PrepareResponseBad.newBuilder().setLastPrepareId(error);
		if (successValue != null) {
			response.setSuccessValue(ValueTranslator.toStoreModelValue(successValue));
		}
		MessagePackage.Builder responseBuilder = MessagePackage.newBuilder();
		responseBuilder.setId(messageId);
		responseBuilder.setIsLast(true);
		responseBuilder.setPrepareResponseBad(response);
		processor.sendResponse(session, responseBuilder.build().toByteArray());
	}

	public synchronized void callGood(StoreModel.Id sid, StoreModel.Value svalue) {
		callGood(IdTranslator.toId(sid), ValueTranslator.toValue(svalue));
	}

	@Override
	public String toString() {
		return "chainPrepare instanceId:" + instanceId;
	}
}
