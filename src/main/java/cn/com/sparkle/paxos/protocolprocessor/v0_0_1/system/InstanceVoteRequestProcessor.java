package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Constants;
import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.model.Value.ValueType;
import cn.com.sparkle.paxos.net.client.system.SystemNetNode;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.InstanceVoteRequest;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback.ChainVoteCallBackV0_0_1;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.util.ValueTranslator;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class InstanceVoteRequestProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(InstanceVoteRequestProcessor.class);

	private Configuration conf;
	private AccountBook aBook;
	private Context context;

	public InstanceVoteRequestProcessor(Context context) {
		super();
		this.conf = context.getConfiguration();
		this.aBook = context.getAccountBook();
		this.context = context;
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasInstanceVoteRequest()) {
			InstanceVoteRequest request = messagePackage.getInstanceVoteRequest();
			if (conf.isDebugLog()) {
				logger.debug("vote instanceId:" + request.getInstanceId());
			}

			if (!request.getVoteId().getAddress().equals(context.getcState().getLastElectionId().getAddress())) {
				ChainVoteCallBackV0_0_1 chainCallback = new ChainVoteCallBackV0_0_1(request.getInstanceId(), 1,
						messagePackage.getId(), session, this, conf.isDebugLog());
				chainCallback.fail();
			} else {

				ChainVoteCallBackV0_0_1 chainCallback = new ChainVoteCallBackV0_0_1(request.getInstanceId(), request.getChainCount() == 0 ? 1 : 2,
						messagePackage.getId(), session, this, conf.isDebugLog());
				doVote(request, chainCallback);
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	public void doVote(InstanceVoteRequest request, final ChainVoteCallBackV0_0_1 chainCallback) {
		final long ct = TimeUtil.currentTimeMillis();
		PrepareRecordRealWriteEvent realWriteEvent = new PrepareRecordRealWriteEvent() {
			@Override
			public void successWrite(long instanceId, InstanceVoteRecord record) {
				chainCallback.call(Constants.VOTE_OK, null);
				if (conf.isDebugLog()) {
					logger.debug("isntanceId:" + instanceId + "vote succeed write file cost:" + (TimeUtil.currentTimeMillis() - ct));
				}
			}

			@Override
			public void instanceSucceeded(long instanceId, SuccessfulRecord record) {
				chainCallback.call(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, ValueTranslator.toValue(record.getV()));
				if (conf.isDebugLog()) {
					logger.debug("isntanceId:" + instanceId + "vote fail, the instance has succeeded!");
				}
			}

			@Override
			public void instanceExecuted(long instanceId) {
				chainCallback.call(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, null);
				if (conf.isDebugLog()) {
					logger.debug("isntanceId:" + instanceId + "vote fail, the instance has succeeded!");
				}
			}
		};
		try {
			long result = aBook.writeVotedRecord(request.getInstanceId(), request.getVoteId(), request.getValue(), realWriteEvent);
			if (result != Constants.FILE_WRITE_SUCCESS) {
				chainCallback.call(result, null);
				if (conf.isDebugLog()) {
					logger.debug("isntanceId:" + request.getInstanceId() + " vote refused");
				}
			} else {
				if (conf.isDebugLog()) {
					logger.debug("vote wait real write event");
				}
			}
			if (request.getChainCount() != 0) {
				//transport to next node

				LinkedList<String> chain = new LinkedList<String>();
				chain.addAll(request.getChainList()); // protobuf return a immutable list,so we need to rebuild a linkedList
				String next = chain.remove(0);
				SystemNetNode node = (SystemNetNode) context.getcState().getSenators().getAllActiveNodes().get(next);
				if (node == null) {
					chainCallback.call(Constants.VOTE_OK, null);
				} else {
					Id id = new Id(request.getVoteId().getAddress(), request.getVoteId().getIncreaseId());
					byte[][] byteValue = new byte[request.getValue().getValuesCount()][];
					for (int i = 0; i < request.getValue().getValuesCount(); ++i) {
						byteValue[i] = request.getValue().getValues(i).toByteArray();
					}
					Value v = new Value(ValueType.getValueType(request.getValue().getType()), byteValue);
					node.sendInstanceVoteRequest(request.getInstanceId(), id, v, chain, chainCallback);
				}
			}
		} catch (Exception e) {
			logger.error("fatal error", e);
			throw new RuntimeException(e);//for close tcp connection
		}
	}

	@Override
	public void sendResponse(PaxosSession session, byte[] response) {
		super.sendResponse(session, response);
	}
}
