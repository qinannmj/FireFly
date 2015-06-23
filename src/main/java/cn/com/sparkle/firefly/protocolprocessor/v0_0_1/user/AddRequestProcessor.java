package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosClient;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.listeners.MasterChangePosEventListener;
import cn.com.sparkle.firefly.event.listeners.MasterDistanceChangeListener;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.AddResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

import com.google.protobuf.ByteString;

public class AddRequestProcessor extends AbstractProtocolV0_0_1Processor implements MasterChangePosEventListener, MasterDistanceChangeListener {

	private final static Logger logger = Logger.getLogger(AddRequestProcessor.class);

	private volatile boolean isMaster = false;

	private volatile PaxosClient client = null;

	private Configuration conf;

	private HandlerInterface handlerInterface;

	private HandlerInterface adminHandlerInterface;

	private ReentrantLock masterChangelock = new ReentrantLock();

	private String masterAddress = "";

	private ProtocolManager protocolManager;

	private Context context;

	public AddRequestProcessor(Context context, HandlerInterface handlerInterface, HandlerInterface adminHandlerInterface) {
		super();
		this.context = context;
		this.conf = context.getConfiguration();
		this.handlerInterface = handlerInterface;
		this.adminHandlerInterface = adminHandlerInterface;
		this.protocolManager = context.getProtocolManager();
		context.getEventsManager().registerListener(this);
	}

	@Override
	public void receive(MessagePackage messagePackage, final PaxosSession session) throws InterruptedException {
		final long packageId = messagePackage.getId();
		if (messagePackage.hasAddRequest()) {
			AddRequest addRequest = new AddRequest(messagePackage.getId(), CommandType.getType(messagePackage.getAddRequest().getCommandType()), messagePackage
					.getAddRequest().getValue().toByteArray(), messagePackage.getAddRequest().getInstanceId());
			boolean instanceIdPass = addRequest.getCommandType().isConsistentlyRead() ? addRequest.getInstanceId() <= context.getInstanceExecutor()
					.getExecutedInstanceId() : true;
			if (addRequest.getCommandType().forceTransportToMaster() || !instanceIdPass) {
				if (isMaster) {
					PaxosOperater operator = session.get(PaxosSessionKeys.PAXOS_OPERATOR_KEY);
					if (operator != null) { //wait until the request remained in queue are processed 
						try {
							operator.waitAllFinish(context.getConfiguration().getTransportTimeout());
							session.remove(PaxosSessionKeys.PAXOS_OPERATOR_KEY);// cancel the channel assigned to this session
						} catch (Exception e) {
							logger.error("unexcepted error", e);
							throw new RuntimeException(e); // just for close this connection
						}
					}
					if (addRequest.getCommandType().isAdmin() && !addRequest.getCommandType().isWrite()) {
						adminHandlerInterface.onCommandReceive(session, addRequest);
					} else {
						handlerInterface.onCommandReceive(session, addRequest);
					}
				} else {
					//transport to master
					if (client != null) {

						PaxosOperater operator = session.get(PaxosSessionKeys.PAXOS_OPERATOR_KEY);
						if (operator == null) {
							operator = client.getOperator();
							session.put(PaxosSessionKeys.PAXOS_OPERATOR_KEY, operator);// assign a channel to master
						}
						//						final PaxosOperater oper = operator;
						try {
							context.getcState().getSelfState().addTransportToMasterCount();
							//The timeout promise the mem is not used excessively
							long instanceId = addRequest.getCommandType().isConsistentlyRead() ? addRequest.getInstanceId() : -1;
							operator.add(addRequest.getValue(), context.getConfiguration().getTransportTimeout(), addRequest.getCommandType(), instanceId,
									new PaxosOperater.CallBack() {
										@Override
										public void callBack(byte[] bytes) {

											MessagePackage.Builder mp = MessagePackage.newBuilder().setIsLast(true).setId(packageId);
											AddResponse.Builder response = AddResponse.newBuilder().setResult(ByteString.copyFrom(bytes))
													.setInstanceId(client.getResponseInstanceId());
											mp.setAddResponse(response);
											sendResponse(session, mp.build().toByteArray());
										}
									});
						} catch (MasterMayBeLostException e) {
							throw new RuntimeException("the master may be lost!");
						}
					} else {
						session.closeSession();
					}
				}
			} else {
				if (addRequest.getCommandType().isAdmin()) {
					adminHandlerInterface.onCommandReceive(session, addRequest);
				} else {
					handlerInterface.onCommandReceive(session, addRequest);
				}
			}
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	@Override
	public void getMasterPos() {
		this.isMaster = true;
	}

	@Override
	public void lostPos() {
		this.isMaster = false;
	}

	@Override
	public void masterChange(String address) {
	}

	@Override
	public void masterDistanceChange(int distance) {
		//find up level node
		SystemNetNode node = context.getcState().getRouteManage().lookupUpLevelNode();

		try {
			masterChangelock.lock();
			String[] senator = null;
			if (node != null) {
				masterAddress = node.getAddress().split(":")[0] + ":" + node.getUserPort();
				if (logger.isDebugEnabled()) {
					logger.debug("transport to " + masterAddress);
				}
				senator = new String[]{masterAddress};
			}else if(context.getcState().getMasterDistance() == 0){
				masterAddress = conf.getSelfAddress();
				senator = new String[]{masterAddress};
			}else{
				senator = new String[]{};
			}
			
			if (client == null) {
				try {
					client = new PaxosClient(senator, conf.getFilePath() + "/service_out_net.prop", conf.getNetLayer(),
							conf.getNetChecksumType(), conf.getHeartBeatInterval(), conf.getTransportTcpNum(), Constants.MAX_MASTER_DISTANCE,
							protocolManager, conf.getTransportSingleTcpMaxWaitingMemSize());
				} catch (Throwable e) {
					logger.error("fatal error", e);
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("change address");
				}
				try {
					client.changeSenator(senator);
				} catch (Throwable e) {
					logger.error("fatal error", e);
				}
			}
		} finally {
			masterChangelock.unlock();
		}
	}

}
