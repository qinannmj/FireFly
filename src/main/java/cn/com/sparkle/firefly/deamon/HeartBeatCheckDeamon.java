package cn.com.sparkle.firefly.deamon;

import java.util.LinkedList;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.AccountBookEventListener;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;
import cn.com.sparkle.firefly.event.listeners.MasterDistanceChangeListener;
import cn.com.sparkle.firefly.event.listeners.NodeStateChangeEventListener;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.state.NodeState;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class HeartBeatCheckDeamon extends Thread implements NodeStateChangeEventListener, AccountBookEventListener, CatchUpEventListener,
		MasterDistanceChangeListener {
	private final static Logger logger = Logger.getLogger(HeartBeatCheckDeamon.class);
	private EventsManager eventsManager;
	private PriorityBlockingQueue<PriorNode> queue = new PriorityBlockingQueue<PriorNode>(100);
	private Context context;
	private boolean isAccountBookInit = false;
	private boolean isUptoDate = false;
	private volatile int distance = Constants.MAX_MASTER_INSTANCE;

	public HeartBeatCheckDeamon(Context context) {
		this.eventsManager = context.getEventsManager();
		this.setName("Paxos-HeatBeat-Checker");
		this.context = context;
		eventsManager.registerListener(this);
	}

	@Override
	public void run() {
		while (true) {
			//			NodesCollection senators = context.getcState().getSenators();
			while (queue.peek() != null && queue.peek().nextExecTime < TimeUtil.currentTimeMillis()) {
				PriorNode priorNode = queue.poll();
				try {
					priorNode.netNode.sendHeartBeat(eventsManager);
					if (distance > 1 || !context.getcState().getSelfState().isSenator()) {
						//send heart to nearby node
						SystemNetNode node = context.getcState().getRouteManage().lookupUpLevelNode();// this node is not master,send active to master by pass
						if (node != null) {
							LinkedList<String> list = new LinkedList<String>();
							list.addAll(context.getcState().getSenators().getValidActiveNodes().keySet());

							NodeState nodeState = new NodeState(context.getConfiguration().getSelfAddress());
							nodeState.setConnected(false);
							nodeState.setInit(isAccountBookInit);
							nodeState.setLastBeatHeatTime(TimeUtil.currentTimeMillis());
							nodeState.setLastCanExecuteInstanceId(context.getAccountBook().getLastCanExecutableInstanceId());
							nodeState.setLastElectionId(context.getcState().getLastElectionId());
							nodeState.setMasterConnected(distance == 1);
							nodeState.setMasterDistance(distance);
							nodeState.setUpToDate(isUptoDate);
							nodeState.setConnectedValidNode(list);
							node.sendActiveHeartBeat(nodeState);
							if (context.getConfiguration().isDebugLog()) {
								logger.debug("send active heartbeat to master by pass!");
							}
						}

					}

					priorNode.nextExecTime = TimeUtil.currentTimeMillis() + priorNode.netNode.getHeartBeatInterval();
					queue.add(priorNode);
				} catch (NetCloseException e) {
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void loseConnect(NetNode nNode) {
	}

	@Override
	public void openConnect(NetNode nNode) {
		PriorNode priorNode = new PriorNode(nNode.getHeartBeatInterval(), (SystemNetNode) nNode);
		if (context.getConfiguration().isDebugLog()) {
			logger.debug("add node to check heart beat queue, interval " + nNode.getHeartBeatInterval());
		}
		queue.add(priorNode);
	}

	@Override
	public void beatHeart(NetNode nNode, NodeState nState) {

	}

	private static class PriorNode implements Comparable<PriorNode> {
		private long nextExecTime;
		private SystemNetNode netNode;

		public PriorNode(long nextExecTime, SystemNetNode netNode) {
			super();
			this.nextExecTime = nextExecTime;
			this.netNode = netNode;
		}

		@Override
		public int compareTo(PriorNode o) {
			if (nextExecTime < o.nextExecTime)
				return -1;
			else if (nextExecTime == o.nextExecTime)
				return 0;
			return 1;
		}

	}

	@Override
	public void activeBeatHeart(String fromAddress, NodeState nState) {
	}

	@Override
	public void accountInit() {
		isAccountBookInit = true;
	}

	@Override
	public void catchUpFail() {
		isUptoDate = false;
	}

	@Override
	public void recoveryFromFail() {
		isUptoDate = true;
	}

	@Override
	public void masterDistanceChange(int distance) {
		this.distance = distance;
	}
}
