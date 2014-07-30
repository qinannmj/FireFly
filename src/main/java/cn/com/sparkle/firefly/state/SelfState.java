package cn.com.sparkle.firefly.state;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.ConfigureEventListener;
import cn.com.sparkle.firefly.model.ElectionId;

public class SelfState implements ConfigureEventListener{
	private final static Logger logger = Logger.getLogger(SelfState.class);
	private volatile ElectionId electionVoteIdBySelf;
	private volatile ElectionId electionPrepareId;
	private volatile long electionLastVoteId = -1;
	private volatile boolean isSenator = false;
	private String selfAddress;

	private ReentrantLock electionPrepareIdLock = new ReentrantLock();
	private AtomicLong transportToMasterCount = new AtomicLong(0);//for statistic
	private long paxosPipelineProcessCount = 0;//for statistic
	private AtomicLong responseCustomAddRequestCount = new AtomicLong(0);//for statistic
	private AtomicLong responseAdminAddRequestCount = new AtomicLong(0);//for statistic
	private long executeFromStartCount = 0; // for statistic
	private long recoverRecordFromMasterLossCount = 0; //for statistic
	private Context context;


	private boolean isDebug;
	
	public SelfState(EventsManager eventManager,String selfAddress){
		eventManager.registerListener(this);
		this.selfAddress = selfAddress;
	}
	public ElectionId getElectionVoteIdBySelf() {
		return electionVoteIdBySelf;
	}

	public void init(Context context) {
		this.context = context;
		electionVoteIdBySelf = new ElectionId("", 0, context.getConfiguration().getConfigNodeSet().getVersion());
		electionPrepareId = new ElectionId("", -1, context.getConfiguration().getConfigNodeSet().getVersion());
		electionLastVoteId = context.getAccountBook().getMaxInstanceIdInVote();
		isDebug = context.getConfiguration().isDebugLog();
		isSenator = context.getConfiguration().getConfigNodeSet().getSenatorsMap().containsKey(context.getConfiguration().getSelfAddress());
	}
	
	public void reInit(){
		if(context != null){
			init(context);
		}
	}
	public ElectionId casElectionPrepareId(ElectionId id, long lastVoteId) {
		try {
			electionPrepareIdLock.lock();
			int compareResult = id.compareTo(electionPrepareId);

			electionLastVoteId = context.getAccountBook().getMaxInstanceIdInVote() > electionLastVoteId ? context.getAccountBook().getMaxInstanceIdInVote()
					: electionLastVoteId;

			if (isDebug) {
				String sample = compareResult == 1 ? ">" : (compareResult == 0 ? "=" : "<");
				logger.debug(String.format("%s%s%s%s%s peerLastVoteId:%s selfLastVoteId:%s peerConfVersion:%s selfConfVersion:%s", id.getIncreaseId(),
						id.getAddress(), sample, electionPrepareId.getIncreaseId(), electionPrepareId.getAddress()

						, lastVoteId, electionLastVoteId, electionPrepareId.getVersion(), context.getConfiguration().getConfigNodeSet().getVersion()));
			}
			if (compareResult >= 0 && lastVoteId >= (electionLastVoteId - Constants.ELECTION_VOTE_ID_TOLERATION)
					&& electionPrepareId.getVersion() >= context.getConfiguration().getConfigNodeSet().getVersion()) {
				electionPrepareId = new ElectionId(id.getAddress(), id.getIncreaseId(), id.getVersion());
				electionLastVoteId = lastVoteId;
				return id;
			}
			return electionPrepareId;
		} finally {
			electionPrepareIdLock.unlock();
		}
	}
	public boolean isSenator(){
		return isSenator;
	}
	public void addTransportToMasterCount() {
		transportToMasterCount.incrementAndGet();
	}

	public void addPaxosPipelineProcessCount() {
		++paxosPipelineProcessCount;
		if (isDebug) {
			if ((paxosPipelineProcessCount) % 1000 == 0) {
				logger.debug("add count " + paxosPipelineProcessCount);
			}
		}
	}

	public void addRecoverRecordFromMasterLossCount() {
		++recoverRecordFromMasterLossCount;
	}

	public void addResponseCustomAddRequestCount() {
		responseCustomAddRequestCount.incrementAndGet();
	}

	public void addResponseAdminAddRequestCount() {
		responseAdminAddRequestCount.incrementAndGet();
	}

	public void addExecuteFromStartCount(int increase) {
		executeFromStartCount += increase;
	}

	public long getRecoverRecordFromMasterLossCount() {
		return recoverRecordFromMasterLossCount;
	}

	public long getTransportToMasterCount() {
		return transportToMasterCount.get();
	}

	public AtomicLong getResponseCustomAddRequestCount() {
		return responseCustomAddRequestCount;
	}

	public AtomicLong getResponseAdminAddRequestCount() {
		return responseAdminAddRequestCount;
	}

	public synchronized long getPaxosPipelineProcessCount() {
		//synchronized for visible for other thread.
		//The reason of not using violate is optimize for add operation in the single thread
		return paxosPipelineProcessCount;
	}

	public synchronized long getExecuteFromStartCount() {
		//synchronized for visible for other thread.
		//The reason of not using violate is optimize for add operation in the single thread
		return executeFromStartCount;
	}

	@Override
	public void senatorsChange(Set<ConfigNode> newSenators, ConfigNode addNode, ConfigNode rmNode, long version) {
		if(addNode != null && addNode.getAddress().equals(selfAddress)){
			isSenator = true;
		}else if(rmNode != null && rmNode.getAddress().equals(selfAddress)){
			isSenator = false;
		}
	}

}
