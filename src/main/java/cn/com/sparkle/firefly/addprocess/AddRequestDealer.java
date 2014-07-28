package cn.com.sparkle.firefly.addprocess;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.InstanceExecuteMaxPackageSizeEvent;
import cn.com.sparkle.firefly.event.listeners.InstanceExecuteEventListener;
import cn.com.sparkle.firefly.event.listeners.InstancePaxosEventListener;
import cn.com.sparkle.firefly.event.listeners.MasterChangePosEventListener;
import cn.com.sparkle.firefly.event.listeners.SpeedControlEventListener;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.firefly.net.netlayer.AttributeKey;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.paxosinstance.InstancePaxosInstance;
import cn.com.sparkle.firefly.paxosinstance.paxossender.InstancePaxosMessageSenderBuilderFactory;
import cn.com.sparkle.firefly.paxosinstance.paxossender.PaxosMessageSender;
import cn.com.sparkle.firefly.paxosinstance.paxossender.builder.SenderBuilder;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;
import cn.com.sparkle.firefly.state.ClusterState;
import cn.com.sparkle.firefly.util.QuorumCalcUtil;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class AddRequestDealer implements InstanceExecuteEventListener, InstancePaxosEventListener, MasterChangePosEventListener, SpeedControlEventListener {

	private final static Logger logger = Logger.getLogger(AddRequestDealer.class);

	private final static AttributeKey<DealState> DEAL_STATE_KEY = new AttributeKey<DealState>("deal_state");

	private final static byte[] NULL_BYTES = new byte[0];

	public final int MAX_TCP_PACKAGE_SIZE;

	public final static int MIN_TCP_PACKAGE_SIZE = 1024;

	public final static int MAX_EXECUTING_INSTANCE_NUM = 100;

	public final static int MAX_SESSION_WAIT_QUEUE_SIZE = 3;

	private int executable_instances_num = MAX_EXECUTING_INSTANCE_NUM;

	public volatile int curTcpPackageByteSize = MIN_TCP_PACKAGE_SIZE;

	private LinkedList<AddRequestPackage> requestQueue = new LinkedList<AddRequestPackage>();

	private ClusterState cState;

	private String selfAddress;

	private AccountBook aBook;

	private int executingInstanceNum = 0;

	private boolean isMaster = false;

	private boolean isOpend = false;

	private Context context;

	private Configuration conf;

	private ReentrantLock lock = new ReentrantLock();

	private long selfIncreaseInstanceId;

	private EventsManager eventsManager;

	private SenderBuilder builder = null;

	private int nullCommandCount = 0;// record the num of null command in
										// waitQueue

	private long withoutPreparePhaseTime;

	public AddRequestDealer(Context context) {
		super();
		this.context = context;
		this.conf = context.getConfiguration();
		this.cState = context.getcState();
		this.selfAddress = conf.getSelfAddress();
		this.aBook = context.getAccountBook();
		this.eventsManager = context.getEventsManager();
		if (conf.getMaxMessagePackageSize() < MIN_TCP_PACKAGE_SIZE * 128) {
			this.MAX_TCP_PACKAGE_SIZE = MIN_TCP_PACKAGE_SIZE * 128;
		} else {
			this.MAX_TCP_PACKAGE_SIZE = conf.getMaxMessagePackageSize();
		}
		eventsManager.registerListener(this);
		builder = InstancePaxosMessageSenderBuilderFactory.getBuilder(conf);
	}

	private void reStartDeal() {
		selfIncreaseInstanceId = aBook.getLastCanExecutableInstanceId();
		// get latest instanceId from others of senators
		NodesCollection senators = cState.getSenators();
		Map<String, NetNode> initedActiveNodes = senators.getValidActiveNodes();

		LookUpLatestInstanceIdCallBack callback = new LookUpLatestInstanceIdCallBack(QuorumCalcUtil.calcQuorumNum(senators.getNodeMembers().size(),
				conf.getDiskMemLost()), initedActiveNodes.size(), conf.isDebugLog()) {
			@Override
			public void finish(long instanceId) {
				try {
					lock.lock();
					if (conf.isDebugLog()) {
						logger.debug("LookUpLatestInstanceIdCallBack result:" + instanceId);
					}
					if (isMaster) {
						if (instanceId == -2) {
							// manifest the master maybe has lost his
							// position,need
							// to notify lost master
							cState.lostAuthorizationOfMaster();
						} else {
							isOpend = true;
							// highSpeedInstanceIdthreshold = instanceId + 1;
							// insert null operation command
							AddRequest nullRequest = new AddRequest(-1, CommandType.ADMIN_WRITE, NULL_BYTES, -1);
							AddRequestPackage nullRequestPackage = new AddRequestPackage(nullRequest, null, context);
							// When one null command has not been finished,the
							// num of null command may be one less than the
							// count needed.
							// So we need make this count add one.So that the
							// num is more than the count needed,when the server
							// get the position of master.
							int newNullCommandCount = (int) (instanceId - selfIncreaseInstanceId);
							if (conf.isDebugLog()) {
								logger.debug("insert " + newNullCommandCount + " null command,current count is " + nullCommandCount);
							}
							for (int i = 0; i < newNullCommandCount - nullCommandCount; i++) {
								requestQueue.add(nullRequestPackage);
								executeNeed();
							}
							executeNeed();
							nullCommandCount = newNullCommandCount > nullCommandCount ? newNullCommandCount : nullCommandCount;
							InstanceExecuteMaxPackageSizeEvent.doMaxPackageSizeChangeEvent(eventsManager, curTcpPackageByteSize);
						}
					}
				} finally {
					lock.unlock();
				}

			}
		};
		for (NetNode node : initedActiveNodes.values()) {
			((SystemNetNode) node).sendLookUpLatestInstanceIdRequest(callback);
		}
	}

	public synchronized long getWithoutPreparePhaseTime() {
		//synchronized for visibility of thread
		return this.withoutPreparePhaseTime;
	}

	public void add(PaxosSession session, AddRequest addRequest) {
		try {
			lock.lock();

			if (!isOpend) {
				session.closeSession();
				return;
			}
			context.getcState().getSelfState().addPaxosPipelineProcessCount();
			DealState dealState = session.get(DEAL_STATE_KEY);
			if (dealState == null) {
				dealState = new DealState(lock, MAX_SESSION_WAIT_QUEUE_SIZE);
				session.put(DEAL_STATE_KEY, dealState);
			}
			if (dealState.isDealing) {
				// wait and merge
				AddRequestPackage lastPackage = dealState.peekLast();
				if (lastPackage != null && lastPackage.getValueByteSize() < curTcpPackageByteSize && !addRequest.getCommandType().isAdmin()
						&& !lastPackage.isManageCommand()) {
					lastPackage.addRequest(addRequest);
				} else {
					dealState.add(new AddRequestPackage(addRequest, session, context));
				}
			} else {
				// no wait
				requestQueue.addLast(new AddRequestPackage(addRequest, session, context));
				dealState.isDealing = true;
			}
			executeNeed();
		} finally {
			lock.unlock();
		}

	}

	public void executeNeed() {
		try {
			lock.lock();
			if (executingInstanceNum == 0) {
				executable_instances_num = MAX_EXECUTING_INSTANCE_NUM; // restore
			}
			int needExecute = executable_instances_num - executingInstanceNum;

			for (int i = 0; i < needExecute && requestQueue.size() > 0; ++i) {
				int curByteCount = 0;
				LinkedList<AddRequestPackage> list = new LinkedList<AddRequestPackage>();
				// merge message from all client
				do {
					AddRequestPackage arp = requestQueue.removeFirst();
					if (arp.isManageCommand()) {
						executable_instances_num = 1;// limit executing number
						needExecute = 0; // stop cycle
						if (executingInstanceNum > 0 && list.size() != 0) {
							requestQueue.addFirst(arp);// restore queue
							break;
						}
					}
					// for null command,there is not DealState,and send a list of size equal 0 indicate a null command
					if (arp.getValueByteSize() != 0) {
						DealState ds = arp.getSession().get(DEAL_STATE_KEY);
						ds.isDealing = true;
						curByteCount += arp.getValueByteSize();
					}
					list.add(arp);

					logger.debug("requestQueue.size()" + requestQueue.size() + " list.size:" + list.size() + " curTcpPackageByteSize" + curTcpPackageByteSize);
					if (arp.isManageCommand() || !context.getConfiguration().isMergeClientRequest()) {
						break; // not to merge, this is will not merge from other node,  so for many client ,to improve rt performance
					}
				} while (requestQueue.size() > 0 && curByteCount + requestQueue.peek().getValueByteSize() < curTcpPackageByteSize);
				if (list.size() != 0) {
					// assign a id
					++selfIncreaseInstanceId;
					if (selfIncreaseInstanceId < aBook.getLastCanExecutableInstanceId()) {
						selfIncreaseInstanceId = aBook.getLastCanExecutableInstanceId() + 1;
					}
					//assign if exist prepare phase
					boolean withPrepare = TimeUtil.currentTimeMillis() < this.withoutPreparePhaseTime;
					// when with prepare phase the increase id is from 1 and it is can be increased.
					// when without prepare phase the increse id is 0 and it is can't be increased.
					Id id = new Id(selfAddress, withPrepare ? 1 : 0);
					while (true) {
						PaxosMessageSender sender = builder.buildSender(context, conf.getPaxosSender());
						if (sender != null) {
							InstancePaxosInstance paxosInstance = new InstancePaxosInstance(sender, selfIncreaseInstanceId, id, list, context);
							paxosInstance.activate(withPrepare);
							break;
						} else {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					++executingInstanceNum;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public int getCurTcpPackageByteSize() {
		return curTcpPackageByteSize;
	}

	@Override
	public void instanceExecuted(cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord record) {
		if (record.getV().getType() == ValueType.ADMIN.getValue()) {
			// this is management of senator/follower command
			try {
				lock.lock();
				if (isOpend) {// the position of this server must be master.
					//calc prepare phase optimized time ,and re-look up the status of cluster
					getMasterPos();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void instanceFail(InstancePaxosInstance instance, Id id, long refuseId, Value value) {
		try {
			lock.lock();
			if (!isOpend) {
				// stop deal process of this instance and close tcp
				// conntion
				--executingInstanceNum;
				for (AddRequestPackage arp : instance.getAddRequestPackages()) {
					arp.closeConnection();
				}

				if (instance.getAddRequestPackages().get(0).getValueByteSize() == 0) {
					// this instance is null command
					--nullCommandCount;
				}
				DealState ds = instance.getAddRequestPackages().get(0).getSession().get(DEAL_STATE_KEY);
				ds.isDealing = false;

			} else if (refuseId == Constants.PAXOS_FAIL_INSTANCE_SUCCEEDED) {
				// The max value of Long indicates the remote peer has
				// succeeded a instance with the same instanceId and the
				// peer has discarded the logs about this instance,so
				// the
				// process of this instance will be stopped.

				try {
					//study actively
					SuccessfulRecord.Builder record = SuccessfulRecord.newBuilder().setV(ValueTranslator.toStoreModelValue(value));
					context.getAccountBook().writeSuccessfulRecord(instance.getInstanceId(), record, null);
				} catch (Throwable e) {
					logger.error("error", e);
				}
				if (instance.getAddRequestPackages().get(0).getValueByteSize() != 0) {
					// this instance is not a null command
					// add all request to deal queue
					for (AddRequestPackage arp : instance.getAddRequestPackages()) {
						requestQueue.addFirst(arp);
					}
				} else {

					// this instance is null command
					--nullCommandCount;
					PaxosSession session = instance.getAddRequestPackages().get(0).getSession();
					if (session != null) {
						logger.debug("session not null");
						DealState ds = session.get(DEAL_STATE_KEY);
						ds.isDealing = false;
					}
					if (conf.isDebugLog()) {
						logger.debug("this instanceId has succeeded! instanceId : " + instance.getInstanceId() + " nullCommandNum:" + nullCommandCount);
					}
				}
				--executingInstanceNum;
				executeNeed();
			} else {
				// Constants.PAXOS_FAIL_FILE_DAMAGED indicates some file is damaged ,
				// and the master can't study from others,and not enough
				// node can provide this instance has succeeded
				if (conf.isDebugLog()) {
					logger.debug("instance fail ,refuse id:" + refuseId);
				}
				boolean withPrepare = TimeUtil.currentTimeMillis() < withoutPreparePhaseTime;
				if (refuseId != Constants.PAXOS_FAIL_TIME_OUT && refuseId != Constants.PAXOS_FAIL_FILE_DAMAGED && withPrepare) {
					id.setIncreaseId(refuseId + 1);
				} else {
					//for whitout prepare phase , the increase id fix at 0
					id.setIncreaseId(0);
					//avoid more cycle
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while (true) {
					//reset sender
					PaxosMessageSender sender = builder.buildSender(context, conf.getPaxosSender());
					if (sender != null) {
						instance.resetActiveNode(sender);
						instance.activate(withPrepare);
						break;
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

		} finally {
			lock.unlock();
		}

	}

	private int dealWait = 0;

	@Override
	public void instanceSuccess(InstancePaxosInstance instance, Value value) {
		try {
			if (conf.isDebugLog()) {
				logger.debug("instance " + instance.getInstanceId() + " succeed!");
			}
			lock.lock();

			--executingInstanceNum;

			if (instance.getAddRequestPackages().get(0).getValueByteSize() == 0) {
				if (instance.getWantAssginValue() != value) {
					context.getcState().getSelfState().addRecoverRecordFromMasterLossCount();
					if (context.getConfiguration().isDebugLog()) {
						logger.debug("recover one command from master loss!");
					}
				} else {
					if (context.getConfiguration().isDebugLog()) {
						logger.debug("null command successed");
					}
				}

				--nullCommandCount;
				executeNeed();
			} else if (instance.getAddRequestPackages().get(0).isManageCommand()) {
				// this is admin senator command,the action
				// must be reacted in executeEvent..
				DealState ds = instance.getAddRequestPackages().get(0).getSession().get(DEAL_STATE_KEY);
				ds.isDealing = false;
			} else {
				if (instance.getWantAssginValue() != value) {
					for (AddRequestPackage arp : instance.getAddRequestPackages()) {
						requestQueue.addFirst(arp);
					}
				} else {
					for (AddRequestPackage arp : instance.getAddRequestPackages()) {
						DealState ds = arp.getSession().get(DEAL_STATE_KEY);
						AddRequestPackage waitArp = ds.pollFirst();
						if (waitArp != null) {
							requestQueue.addLast(waitArp);
							++dealWait;
							logger.debug("dealWait:" + dealWait + "ds:" + ds.size());
						} else {
							ds.isDealing = false;
						}
					}
				}
				executeNeed();
			}

		} finally {
			lock.unlock();
		}

	}

	@Override
	public void instanceStart(InstancePaxosInstance instance) {
	}

	@Override
	public void getMasterPos() {
		try {
			lock.lock();
			isMaster = true;
			withoutPreparePhaseTime = TimeUtil.currentTimeMillis() + 5 * Constants.MAX_HEART_BEAT_INTERVAL;
			if (context.getConfiguration().isDebugLog()) {
				logger.debug("withoutPreparePhaseTime: " + withoutPreparePhaseTime + " curtime: " + TimeUtil.currentTimeMillis());
			}
			reStartDeal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void lostPos() {
		try {
			lock.lock();
			isMaster = false;
			isOpend = false;
			withoutPreparePhaseTime = Long.MAX_VALUE;
			// clear queue of waiting to deal
			for (AddRequestPackage arp : requestQueue) {
				if (arp != null) {
					arp.closeConnection();
				}
			}
			requestQueue.clear();
			if (context.getConfiguration().isDebugLog()) {
				logger.debug("lost position!");
			}
		} finally {
			lock.unlock();
		}

	}

	@Override
	public void masterChange(String address) {
	}

	@Override
	public void suggestMaxPackageSize(int suggestSize) {
		suggestSize = Math.max(Math.min(suggestSize, MAX_TCP_PACKAGE_SIZE), MIN_TCP_PACKAGE_SIZE);
		if (suggestSize != curTcpPackageByteSize) {
			curTcpPackageByteSize = suggestSize;
			InstanceExecuteMaxPackageSizeEvent.doMaxPackageSizeChangeEvent(eventsManager, curTcpPackageByteSize);
		}
	}

	public PaxosMessageSender getSender() {
		return builder.buildSender(context, conf.getPaxosSender());
	}
}
