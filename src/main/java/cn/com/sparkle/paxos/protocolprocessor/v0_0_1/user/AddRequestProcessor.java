package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.user;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Constants;
import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.client.MasterMayBeLostException;
import cn.com.sparkle.paxos.client.PaxosClient;
import cn.com.sparkle.paxos.client.PaxosOperater;
import cn.com.sparkle.paxos.config.ConfigNode;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.listeners.MasterChangePosEventListener;
import cn.com.sparkle.paxos.event.listeners.MasterDistanceChangeListener;
import cn.com.sparkle.paxos.handlerinterface.HandlerInterface;
import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.model.AddRequest.CommandType;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolManager;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.AddResponse;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

import com.google.protobuf.ByteString;

public class AddRequestProcessor extends AbstractProtocolV0_0_1Processor implements MasterChangePosEventListener, MasterDistanceChangeListener {

	private final static Logger logger = Logger.getLogger(AddRequestProcessor.class);

	private volatile boolean isMaster = false;

	private volatile PaxosClient client = null;

	private Configuration conf;

	private HandlerInterface handlerInterface;

	private HandlerInterface adminHandlerInterface;

	private ReentrantLock masterChangelock = new ReentrantLock();

	private String masterAddress;

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
					.getAddRequest().getValue().toByteArray());
			if (addRequest.forceTransportToMaster()) {
				if (isMaster) {
					PaxosOperater operator = session.get(PaxosSessionKeys.PAXOS_OPERATOR_KEY);
					if (operator != null) { //wait until the request remained in queue are processed 
						try {
							operator.waitAllFinish(context.getConfiguration().getTransportTimeout());
							session.put(PaxosSessionKeys.PAXOS_OPERATOR_KEY, null);// cancel the channel assigned to this session
						} catch (Exception e) {
							logger.error("unexcepted error", e);
							throw new RuntimeException(e); // just for close this connection
						}
					}
					if (addRequest.isAdmin() && !addRequest.isWrite()) {
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
						try {
							context.getcState().getSelfState().addTransportToMasterCount();
							//The timeout promise the mem is not used excessively
							operator.add(addRequest.getValue(), context.getConfiguration().getTransportTimeout(), addRequest.getCommandType(),
									new PaxosOperater.CallBack() {
										@Override
										public void callBack(byte[] bytes) {

											MessagePackage.Builder mp = MessagePackage.newBuilder().setIsLast(true).setId(packageId);
											AddResponse.Builder response = AddResponse.newBuilder().setResult(ByteString.copyFrom(bytes));
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
				if(addRequest.isAdmin()){
					adminHandlerInterface.onCommandReceive(session, addRequest);
				}else{
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
		String address = context.getcState().getRouteManage().lookupUpLevelNodeAddress(distance);
		if (address == null && distance == 0) {
			address = conf.getSelfAddress(); // self is master
		}
		if (conf.isDebugLog()) {
			logger.debug("distance change to " + distance + " uplevel node:" + address);
		}

		if (address != null) {

			try {
				masterChangelock.lock();

				for (ConfigNode cnode : conf.getConfigNodeSet().getSenators()) {
					if ((cnode.getAddress()).equals(address)) {
						masterAddress = cnode.getIp() + ":" + cnode.getClientPort();
					}
				}
				if (conf.isDebugLog()) {
					logger.debug("transport to " + masterAddress);
				}
				if (client == null && masterAddress != null) {
					try {
						client = new PaxosClient(new String[] { masterAddress }, conf.getFilePath() + "/service_out_net.prop", conf.getNetLayer(),
								conf.getNetChecksumType(), conf.getHeartBeatInterval(), conf.getTransportTcpNum(), Constants.MAX_MASTER_INSTANCE,
								protocolManager, conf.isDebugLog());
					} catch (Throwable e) {
						logger.error("fatal error", e);
					}
				} else {
					if (conf.isDebugLog()) {
						logger.debug("change address");
					}
					try {
						client.changeSenator(new String[] { masterAddress });
					} catch (Throwable e) {
						logger.error("fatal error", e);
					}
				}
			} finally {
				masterChangelock.unlock();
			}
		}
	}

}
