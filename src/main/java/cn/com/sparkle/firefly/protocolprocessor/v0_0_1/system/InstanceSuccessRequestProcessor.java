package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.SuccessTransportConfig;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceSuccessMessage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceSuccessTransport;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;

public class InstanceSuccessRequestProcessor extends AbstractProtocolV0_0_1Processor {
	private final static Logger logger = Logger.getLogger(InstanceSuccessRequestProcessor.class);

	private AccountBook aBook;
	private Configuration conf;
	private Context context;

	public InstanceSuccessRequestProcessor(Context context) {
		super();
		this.aBook = context.getAccountBook();
		this.conf = context.getConfiguration();
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see cn.com.sparkle.paxos.net.protocolprocessor.ProtocolProcessor#receive(java.lang.Object, cn.com.sparkle.paxos.net.netlayer.PaxosSession)
	 */
	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {
		if (messagePackage.hasInstanceSuccessMessage()) {
			InstanceSuccessMessage message = messagePackage.getInstanceSuccessMessage();

			try {

				SuccessfulRecord.Builder record = SuccessfulRecord.newBuilder().setHighestVoteNum(message.getHighestVoteNum());

				if (message.hasValue()) {
					record.setV(message.getValue());//set value to record
				}
				//write to store 
				aBook.writeSuccessfulRecord(message.getId(), record, null);

				//notify to other node
				List<String> notifyList = message.getNotifyAddressList();
				List<InstanceSuccessTransport> notifyChain = message.getNotifyChainList();
				// if the successful record has write in past , the record.hasV return false and stop transport to other node
				if ((notifyList.size() != 0 || notifyChain.size() != 0) && record.hasV()) {
					Id id = new Id(message.getHighestVoteNum().getAddress(), message.getHighestVoteNum().getIncreaseId());
					Value v = ValueTranslator.toValue(record.getV());
					if (notifyChain.size() != 0) {
						//this node need to notify the next node in the chain
						LinkedList<InstanceSuccessTransport> notifyChainList = new LinkedList<InstanceSuccessTransport>();
						notifyChainList.addAll(notifyChain);
						InstanceSuccessTransport transport = notifyChainList.remove(0);
						if (transport.getIsTransValue()) {
							trySendSuccess(transport.getAddress(), message.getId(), id, v, transport.getNotifyAddressList(), notifyChainList);
							if (conf.isDebugLog()) {
								logger.debug("transport with value to " + transport.getAddress());
							}
						} else {
							trySendSuccess(transport.getAddress(), message.getId(), id, null, transport.getNotifyAddressList(), notifyChainList);
							if (conf.isDebugLog()) {
								logger.debug("transport without value to " + transport.getAddress());
							}
						}
					}
					if (notifyList.size() != 0) {
						//this node need to notify nodes not in quorum
						for (String address : notifyList) {
							trySendSuccess(address, message.getId(), id, v, null, null);
							if (conf.isDebugLog()) {
								logger.debug("notify with value to " + address);
							}
						}
					}

				}

				if (messagePackage.getId() != -1) {
					//this is a request needed to response
					super.sendResponse(session, MessagePackage.newBuilder().setId(messagePackage.getId()).setIsLast(true).build().toByteArray());
				}

				if (conf.isDebugLog()) {
					logger.debug("instanceId:" + message.getId() + " succeeded");
				}
			} catch (IOException e) {
				logger.error("fatal error", e);
			} catch (UnsupportedChecksumAlgorithm e) {
				logger.error("fatal error", e);
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	private void trySendSuccess(String address, long instanceId, Id id, Value value, List<String> notifyList, List<InstanceSuccessTransport> notifyChain) {
		SystemNetNode node = (SystemNetNode) context.getcState().getSenators().getValidActiveNodes().get(address);
		List<SuccessTransportConfig> transportConfigList = null;
		if (notifyChain != null && notifyChain.size() != 0) { //transform InstanceSuccessTransport to SuccessTransportConfig
			transportConfigList = new LinkedList<SuccessTransportConfig>();
			for (InstanceSuccessTransport transport : notifyChain) {
				SuccessTransportConfig config = new SuccessTransportConfig(transport.getAddress(), transport.getNotifyAddressList(),
						transport.getIsTransValue());
				transportConfigList.add(config);
			}
		}

		if (node != null) {
			try {
				node.sendInstanceSuccessMessage(instanceId, id, value, notifyList, transportConfigList);
			} catch (InterruptedException e) {
				logger.error("unexcepted error", e);
			}
		}
	}

}
