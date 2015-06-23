package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceVoteRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ValueTrunk;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstanceVoteRequestProcessor.ValueTransportConfig;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.ChainVoteCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.votevaluetransport.ValueTransportPipeState;
import cn.com.sparkle.firefly.protocolprocessor.votevaluetransport.ValueTransportPipeState.InstanceVoteValueState;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class InstanceVoteValueTransportProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(InstanceVoteValueTransportProcessor.class);

	private Configuration conf;
	private AccountBook aBook;
	private Context context;

	public InstanceVoteValueTransportProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.aBook = context.getAccountBook();
		this.context = context;
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasValueTrunk()) {
			ValueTrunk valueTrunk = messagePackage.getValueTrunk();
			byte[] partValue = valueTrunk.getPart().toByteArray();
			long messageId = messagePackage.getId();
			ValueTransportPipeState valueState = session.get(PaxosSessionKeys.VOTE_VAlUE_TRANSPORT_PIPE_STATE);
			InstanceVoteValueState instanceState = valueState.getState(messageId);

			

			instanceState.getV().fill(partValue);

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("value transport pipe recv : %s / %s messageId:%s", instanceState.getV().length(), instanceState.getDataLength(),messagePackage.getId()));
			}
			
			ValueTransportConfig config = (ValueTransportConfig) instanceState.getObj();
			if (instanceState.getV().length() == instanceState.getDataLength()) {
				
				//finish and clear mem
				valueState.unRegister(messageId);
				InstanceVoteRequest request = config.getRequest();
				if (!request.getVoteId().getAddress().equals(context.getcState().getLastElectionId().getAddress())) {
					config.getCallback().fail();//fast fail
				} else {
					writeLog(request, instanceState.getV(), config.getCallback(), session);
				}
				transport(config, partValue, config.getCallback());//transport to other node
			} else {
				//transport to other node
				transport(config, partValue, null);
			}

		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	private void transport(ValueTransportConfig config, byte[] bytes, ChainVoteCallBackV0_0_1 callback) {
		SystemNetNode node = config.getNode();
		if (node != null) {
			config.getNode().sendVoteValueTrunk(config.getMessageId(), callback, bytes, 0, bytes.length);
		}
	}

	private void writeLog(InstanceVoteRequest request, final Value value, final ChainVoteCallBackV0_0_1 chainCallback, final PaxosSession session) {
		final long ct = TimeUtil.currentTimeMillis();
		PrepareRecordRealWriteEvent realWriteEvent = new PrepareRecordRealWriteEvent() {
			@Override
			public void successWrite(long instanceId, InstanceVoteRecord record) {
				chainCallback.call(Constants.VOTE_OK, null);
				if (logger.isDebugEnabled()) {
					logger.debug("isntanceId:" + instanceId + "vote succeed write file cost:" + (TimeUtil.currentTimeMillis() - ct));
				}
			}

			@Override
			public void instanceSucceeded(long instanceId, SuccessfulRecord record) {
				chainCallback.call(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, ValueTranslator.toValue(record.getV()));
				if (logger.isDebugEnabled()) {
					logger.debug("isntanceId:" + instanceId + "vote fail, the instance has succeeded!");
				}
			}

			@Override
			public void instanceExecuted(long instanceId) {
				chainCallback.call(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, null);
				if (logger.isDebugEnabled()) {
					logger.debug("isntanceId:" + instanceId + "vote fail, the instance has succeeded!");
				}
			}
		};

		try {
			long result = aBook
					.writeVotedRecord(request.getInstanceId(), request.getVoteId(), ValueTranslator.toStoreModelValue(value).build(), realWriteEvent);
			if (result != Constants.FILE_WRITE_SUCCESS) {
				chainCallback.call(result, null);
				if (logger.isDebugEnabled()) {
					logger.debug("isntanceId:" + request.getInstanceId() + " vote refused");
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("vote wait real write event");
				}
			}
		} catch (Exception e) {
			logger.error("fatal error", e);
			throw new RuntimeException(e);//for close tcp connection
		}
	}
}
