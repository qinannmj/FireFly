package cn.com.sparkle.firefly.deamon;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;
import cn.com.sparkle.firefly.event.listeners.ElectionEventListener;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.paxosinstance.ElectionPaxosInstance;
import cn.com.sparkle.firefly.state.ClusterState;
import cn.com.sparkle.firefly.state.NodeState;
import cn.com.sparkle.firefly.util.QuorumCalcUtil;

public class CheckMasterSenatorStateDeamon implements Runnable, CatchUpEventListener, ElectionEventListener {
	private final static Logger logger = Logger.getLogger(CheckMasterSenatorStateDeamon.class);

	private ClusterState cState;
	// assume this system's state is failure when initiate this system.
	private boolean checkable = false;
	// private ClientNetHandler handler;
	private ReentrantLock lock = new ReentrantLock();

	private String selfAddress;
	private Configuration conf;
	private Random random = new Random();
	private int conflictSleepTime;
	private Context context;
	private volatile boolean isActiveElection = false;

	public CheckMasterSenatorStateDeamon(Context context) {
		this.conf = context.getConfiguration();
		this.cState = context.getcState();
		this.selfAddress = conf.getSelfAddress();
		this.conflictSleepTime = 200 * conf.getElectionPriority();
		this.context = context;
		context.getEventsManager().registerListener(this);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(conflictSleepTime);
			} catch (InterruptedException e) {
			}
			try {
				lock.lock();

				NodesCollection senators = cState.getSenators();
				int quorum = QuorumCalcUtil.calcQuorumNum(senators.getNodeMembers().size(), conf.getDiskMemLost());
				Map<String, NetNode> validActiveSenator = senators.getValidActiveNodes();
				ElectionId maxElectionid = cState.getLastElectionId();
				int maxElectionIdLostCount = 0;
				for (NetNode nnode : validActiveSenator.values()) {
					NodeState ns = senators.getNodeStates().get(nnode.getAddress());
					if (ns.getLastElectionId().compareTo(maxElectionid) == 0 && !ns.isMasterConnected()) {
						++maxElectionIdLostCount;
					} else if (ns.getLastElectionId().compareTo(maxElectionid) == 1) {
						maxElectionid = ns.getLastElectionId();
						maxElectionIdLostCount = 0;
					}
				}
				// active lost the position of master
				if (validActiveSenator.size() < quorum) {
					cState.lostAuthorizationOfMaster();
				} else {
					// active study newest master
					if (!maxElectionid.getAddress().equals(conf.getSelfAddress()) || context.getcState().getSelfState().isSenator()) {
						//avoid study self's address form other's nodestate when self is not senator
						cState.changeLastElectionId(maxElectionid);
					}
				}

				// memory barrier
				if (!checkable || !context.getcState().getSelfState().isSenator()) {
					continue;
				}
				// if and only if the number that itself can connect more than
				// quorum , it can elect itself as master.
				// And when the number that itself can connect more than quorum
				// , it
				// must can get information from others about itself has been or
				// not
				// a master.
				// If it has been a master and it lost its position actively, it
				// must start a election that elect itself as a master again to
				// remind others that the master has been lost.
				boolean isMasterLost = (maxElectionIdLostCount >= quorum || (cState.getLastElectionId().getIncreaseId() == -1 && selfAddress
						.equals(maxElectionid.getAddress()))) && validActiveSenator.size() >= quorum;
				if (isMasterLost || isActiveElection) {
					if (isMasterLost) {
						logger.info("master is lost!!!");
					} else {
						isActiveElection = false;
						logger.info("active elect self be the master");
					}
					if (conf.isElectSelfMaster() && !conf.isArbitrator()) {
						startSelection(validActiveSenator, quorum);
					} else {
						if (logger.isDebugEnabled()) {
							logger.info("elect_self_master=false , give up election!");
						}
					}
					try {
						//wait all node report his status to avoid elect itself repeatly
						Thread.sleep(Constants.MAX_HEART_BEAT_INTERVAL);
					} catch (InterruptedException e) {
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}

	private void startSelection(Map<String, NetNode> activeSenator, int quorum) {
		// fix address to selfaddress
		cState.getSelfState().getElectionVoteIdBySelf().setAddress(selfAddress);
		if (logger.isDebugEnabled()) {
			logger.debug("start election, instanceId:" + cState.getSelfState().getElectionVoteIdBySelf().getIncreaseId());
		}
		ElectionPaxosInstance paxosInstance = new ElectionPaxosInstance(activeSenator, quorum, cState.getSelfState().getElectionVoteIdBySelf(), context,
				selfAddress);
		Future<Boolean> f = paxosInstance.activate();
		boolean isSuccess = false;
		try {
			isSuccess = f.get();
		} catch (InterruptedException e1) {
			logger.error("fatal error", e1);
		} catch (ExecutionException e1) {
			logger.error("fatal error", e1);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("election voteId.increaseId:" + cState.getSelfState().getElectionVoteIdBySelf().getIncreaseId() + "  election isSuccessful:"
					+ isSuccess);
		}
		if (!isSuccess) {
			try {
				Thread.sleep(random.nextInt(500) + conflictSleepTime);
			} catch (InterruptedException e) {
			}
		}

	}

	@Override
	public void recoveryFromFail() {
		try {
			lock.lock();
			checkable = true;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void catchUpFail() {
		try {
			lock.lock();
			checkable = false;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void reElection() {
		this.isActiveElection = true;
	}
}
