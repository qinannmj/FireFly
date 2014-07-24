package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Constants;
import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.net.client.system.SystemNetNode;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.InstancePrepareRequest;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback.ChainPrepareCallBackV0_0_1;
import cn.com.sparkle.paxos.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.Id;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.paxos.stablestorage.util.ValueTranslator;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class InstancePrepareRequestProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(InstancePrepareRequestProcessor.class);
	private Context context;

	public InstancePrepareRequestProcessor(Context context) {
		super();
		this.context = context;
	}

	@Override
	public void sendResponse(PaxosSession session, byte[] response) { // expose this method to chainPrepareCallback
		super.sendResponse(session, response);
	}

	@Override
	public void receive(final MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		if (messagePackage.hasInstancePrepareRequest()) {

			InstancePrepareRequest request = messagePackage.getInstancePrepareRequest();
			if (context.getConfiguration().isDebugLog()) {
				logger.debug("prepare instanceId:" + request.getInstanceId());
			}
			//check master address
			if (!request.getId().getAddress().equals(context.getcState().getLastElectionId().getAddress())) {
				ChainPrepareCallBackV0_0_1 chainPrepareCallBack = new ChainPrepareCallBackV0_0_1(this, request.getInstanceId(), 1,
						messagePackage.getId(), session, context.getConfiguration().isDebugLog());
				//response time out to requester
				chainPrepareCallBack.callBad(Constants.PAXOS_FAIL_TIME_OUT, null);
			} else {
				int needResponseCount = request.getChainCount() == 0 ? 1 : 2;
				ChainPrepareCallBackV0_0_1 chainPrepareCallBack = new ChainPrepareCallBackV0_0_1(this, request.getInstanceId(), needResponseCount,
						messagePackage.getId(), session, context.getConfiguration().isDebugLog());
				doPrepare(request,chainPrepareCallBack);
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}
	private void doPrepare(InstancePrepareRequest request,final ChainPrepareCallBackV0_0_1 chainPrepareCallBack){
		try {
			

			final long ct = TimeUtil.currentTimeMillis();
			long result = context.getAccountBook().writePrepareRecord(request.getInstanceId(), request.getId(), new PrepareRecordRealWriteEvent() {
				@Override
				public void successWrite(long instanceId, InstanceVoteRecord record) {
					Id id = record.hasHighestVotedNum() ? record.getHighestVotedNum() : record.getHighestJoinNum();
					Value value = record.hasHighestValue() ? record.getHighestValue() : null;
					chainPrepareCallBack.callGood(id, value);
					if (context.getConfiguration().isDebugLog()) {
						logger.debug("instanceId: " + instanceId + " prepare succeeded write file cost:" + (TimeUtil.currentTimeMillis() - ct)
								+ " value isNull:" + (value == null));
					}
				}

				@Override
				public void instanceSucceeded(long instanceId, SuccessfulRecord record) {
					//indicate the instance has succeed,and has not put to execute queue
					chainPrepareCallBack.callBad(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, ValueTranslator.toValue(record.getV()));
					if (context.getConfiguration().isDebugLog()) {
						logger.debug("prepare instanceId:" + instanceId + " refused.the instance has succeeded.");
					}
				}

				@Override
				public void instanceExecuted(long instanceId) {
					//there is an other master existed in the cluster.
					//in this condition , this node can response timeout to master launched this instance vote,
					//The the master will study this instance through the process of catch up.
					chainPrepareCallBack.callBad(Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED, null);
					if (context.getConfiguration().isDebugLog()) {
						logger.debug("prepare instanceId:" + instanceId + " refused.the instance has succeeded.");
					}
				}
			});

			if (result != Constants.FILE_WRITE_SUCCESS) {// failed
				chainPrepareCallBack.callBad(result, null);
				if (context.getConfiguration().isDebugLog()) {
					logger.debug("prepare refused: instanceId " + request.getInstanceId() + "  conflict with highestJoinNum:" + result
							+ " exceptedJoinNum:" + request.getId().getIncreaseId() + ":" + request.getId().getAddress());
				}
			}

			if (request.getChainCount() != 0) {
				//transport to next node in the chain
				LinkedList<String> targetChain = new LinkedList<String>();
				targetChain.addAll(request.getChainList());
				String next = targetChain.remove(0);
				SystemNetNode node = (SystemNetNode) context.getcState().getSenators().getAllActiveNodes().get(next);
				if (node == null) {
					chainPrepareCallBack.fail();
				} else {
					//start transport
					cn.com.sparkle.paxos.model.Id id = new cn.com.sparkle.paxos.model.Id(request.getId().getAddress(), request.getId().getIncreaseId());
					node.sendInstancePrepareRequest(id, request.getInstanceId(), targetChain, chainPrepareCallBack);
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);//for close tcp connection
		}
	}
}
