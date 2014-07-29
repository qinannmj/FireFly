package cn.com.sparkle.firefly.deamon;

import java.util.Map;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.CatchUpEvent;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.state.ClusterState;
import cn.com.sparkle.firefly.state.NodeState;
import cn.com.sparkle.firefly.util.QuorumCalcUtil;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class CatchUpDeamon implements Runnable {
	private final Logger logger = Logger.getLogger(CatchUpDeamon.class);

	private ClusterState cState;
	private Configuration conf;
	private AccountBook aBook;
	private EventsManager eventsManager;
	private String selfAddress;

	public CatchUpDeamon(Context context) {
		this.eventsManager = context.getEventsManager();
		this.cState = context.getcState();
		this.conf = context.getConfiguration();
		this.selfAddress = conf.getSelfAddress();
		this.aBook = context.getAccountBook();
	}

	@Override
	public void run() {
		int curCatchUpState = CatchUpEvent.FAIL_CATCH_UP;
		long lastCanExecuteInstanceIdRecord = -1;
		long lastInstanceIdCheckTime = 0;
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			NodesCollection senators = cState.getSenators();
			Map<String, NetNode> allActiveSenator = senators.getAllActiveNodes();
			long lastCanExecuteInstanceId = aBook.getLastCanExecutableInstanceId();
			if (lastCanExecuteInstanceId != lastCanExecuteInstanceIdRecord) {
				lastCanExecuteInstanceIdRecord = lastCanExecuteInstanceId;
				lastInstanceIdCheckTime = TimeUtil.currentTimeMillis();
			} else if (TimeUtil.currentTimeMillis() - lastInstanceIdCheckTime > conf.getCatchUpDelay()) {
				// find max instance id from cluster
				long maxInstanceId = -1;
				for (NetNode nnode : allActiveSenator.values()) {
					NodeState nodeState = senators.getNodeStates().get(nnode.getAddress());
					if (nodeState.isConnected() && !nodeState.getAddress().equals(selfAddress)) {
						if (nodeState.getLastCanExecuteInstanceId() > maxInstanceId) {
							maxInstanceId = nodeState.getLastCanExecuteInstanceId();
						}
					}
				}
				long selfInstanceId = aBook.getLastCanExecutableInstanceId();
				long tempselfInstanceId = -1;
				if (conf.isDebugLog()) {

					logger.debug("before catchUp state( maxInstanceId:" + maxInstanceId + " selfInstanceId:" + selfInstanceId + " activeNodeSize:"
							+ allActiveSenator.size() + " initedNodeSze:" + senators.getValidActiveNodes().size() + ")");
				}
				boolean isGiveupCatchup = false;

				while (maxInstanceId > selfInstanceId) {
					tempselfInstanceId = selfInstanceId;
					for (int j = 0; j < 2; ++j) {
						//for j == 0 , give prior to study from nodes of same room with self
						for (NetNode nnode : allActiveSenator.values()) {
							if (nnode.getAddress().equals(selfAddress) || (j == 0 && !senators.isSameRoom(selfAddress, nnode.getAddress()))) {
								continue;
							}
							NodeState nodeState = senators.getNodeStates().get(nnode.getAddress());
							long nnodeInstanceId = nodeState.getLastCanExecuteInstanceId();
							nnodeInstanceId = maxInstanceId > nnodeInstanceId ? nnodeInstanceId : maxInstanceId;
							// try study newer instance than self's
							while (nnodeInstanceId > selfInstanceId) {
								if (conf.isDebugLog()) {
									logger.debug("catch up from " + nnode.getAddress());
								}
								long catchUpInstanceId = aBook.getFisrtInstnaceIdOfWaitInsertToExecuteQueue();
								if (catchUpInstanceId == -1 || catchUpInstanceId > nnodeInstanceId) {
									catchUpInstanceId = Math.min(nnodeInstanceId, selfInstanceId + Constants.CATCH_STOP_NUM);
								}
								// start process of catch up
								catchUp(nnode, selfInstanceId + 1, (int) (catchUpInstanceId - selfInstanceId));

								selfInstanceId = aBook.getLastCanExecutableInstanceId();
								if (selfInstanceId < catchUpInstanceId) { // the
																			// peer
																			// can't
																			// provide
																			// enough
																			// record
									break;
								} else if (selfInstanceId > catchUpInstanceId) {
									// this condition hints this node are receive
									// new record by other way,
									// so, we will give up the process of catch up
									// to avoid receive same record repeatedly
									isGiveupCatchup = true;
									break;
								}
							}
							if(isGiveupCatchup){
								break;
							}
						}
						if(isGiveupCatchup){
							break;
						}
					}
					if (isGiveupCatchup) {
						break;// give up
					} else if (tempselfInstanceId == selfInstanceId) {
						// if the selfInstanceId is not changed, it will can't
						// study
						// instance from others
						break;
					}

				}
				if (conf.isDebugLog()) {
					logger.debug("after catchUp state( maxInstanceId:" + maxInstanceId + " selfInstanceId:" + selfInstanceId + " activeNodeSize:"
							+ allActiveSenator.size() + " initedNodeSze:" + senators.getValidActiveNodes().size() + ")");
				}

				if (!isGiveupCatchup) {
					int quorum = QuorumCalcUtil.calcQuorumNum(senators.getNodeMembers().size(), conf.getDiskMemLost()) - 1;// except
																															// self
					// state is assigned by heart beat
					if (selfInstanceId >= maxInstanceId) {
						if (curCatchUpState == CatchUpEvent.FAIL_CATCH_UP) {
							int newValidCount = 0;
							long newMaxInstanceId = -1;
							NodeState nodeState = null;
							for (NetNode nnode : allActiveSenator.values()) {
								nodeState = senators.getNodeStates().get(nnode.getAddress());
								if (nodeState.isConnected() && !nodeState.getAddress().equals(selfAddress)) {
									if (nodeState.getLastCanExecuteInstanceId() > newMaxInstanceId) {
										newMaxInstanceId = nodeState.getLastCanExecuteInstanceId();
									}
									if (nodeState.isInit()) {
										++newValidCount;
									}
								}
							}
							if (newMaxInstanceId - selfInstanceId <= Constants.CATCH_STOP_NUM
									&& (newValidCount >= quorum || (nodeState != null && nodeState.isUpToDate()))) {
								// this condition can promise this node not
								// fall behind other nodes too many
								curCatchUpState = CatchUpEvent.RECOVERY_FROM_CATCH_UP;
								CatchUpEvent.doRecoveryFromFailEvent(eventsManager);
								if (conf.isDebugLog()) {
									logger.debug("catch up success");
								}
							}
						}
					} else if (tempselfInstanceId == selfInstanceId) {
						// fail to catch up,activate failure method
						if (curCatchUpState == CatchUpEvent.RECOVERY_FROM_CATCH_UP) {
							curCatchUpState = CatchUpEvent.FAIL_CATCH_UP;
							CatchUpEvent.doCatchUpFailEvent(eventsManager);
							if (conf.isDebugLog()) {
								logger.debug("catch up fail");
							}
						}
					}
				} else if (conf.isDebugLog()) {
					logger.debug("give up catch up!");
				}
				lastInstanceIdCheckTime = TimeUtil.currentTimeMillis();
			}
		}
	}

	private void catchUp(NetNode netNode, long startInstanceId, final int size) {
		SystemNetNode node = (SystemNetNode) netNode;
		CatchUpCallBack callback = new CatchUpCallBack(conf, aBook);
		node.sendCatchUpRequest(startInstanceId, size, callback);
		if (conf.isDebugLog()) {
			logger.debug(String.format("catch up [%s,%s] from %s", startInstanceId, startInstanceId + size, netNode.getAddress()));
		}
		try {
			callback.waitFinish();
		} catch (InterruptedException e) {
			logger.error("unexcepted error", e);
		}
	}

}
