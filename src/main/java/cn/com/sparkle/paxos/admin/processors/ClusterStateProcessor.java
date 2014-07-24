package cn.com.sparkle.paxos.admin.processors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Constants;
import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.NodesCollection;
import cn.com.sparkle.paxos.admin.AbstractAdminProcessor;
import cn.com.sparkle.paxos.admin.AdminLookupHandler;
import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.paxosinstance.paxossender.PaxosMessageSender;
import cn.com.sparkle.paxos.state.NodeState;
import cn.com.sparkle.raptor.core.util.TimeUtil;

/**
 * target command targetElectionIp
 * @author qinan.qn
 *
 */
public class ClusterStateProcessor extends AbstractAdminProcessor {
	private final static Logger logger = Logger.getLogger(ClusterStateProcessor.class);
	private final static String SENATOR_FORMAT = "%-35s%-20s%-20s%-25s%-17s%-20s%-20s";
	private final static String FOLLOWER_FORMAT = "%-35s%-20s%-25s%-17s%-20s%-20s";
	private Context context;

	public ClusterStateProcessor(Context context) {
		this.context = context;
	}

	@Override
	public String processComm(String[] command, AdminLookupHandler handler, PaxosSession session, AddRequest request) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		pw.println("cluster status:");
		pw.println(String.format("paxos-sender-type=%s", context.getConfiguration().getPaxosSender()));
		PaxosMessageSender sender = context.getAddRequestDealer().getSender();
		pw.println(String.format("sender-info=%s", sender == null ? "" : sender.linkInfo()));
		long remainTime = (context.getAddRequestDealer().getWithoutPreparePhaseTime() - TimeUtil.currentTimeMillis()) / 1000;
		if (context.getConfiguration().isDebugLog()) {
			logger.debug("withoutPreparePhaseTime: " + context.getAddRequestDealer().getWithoutPreparePhaseTime() + "  remainTime: " + remainTime);
		}
		pw.println(String.format("prepare-optimized-status=%s", remainTime <= 0 ? "optimized" : " remained " + remainTime + "s"));
		pw.println();
		pw.println("senators status:");
		pw.println(String.format(SENATOR_FORMAT, "id","room", "isMasterConnected", "lastHeartTime", "maxLogId", "master-distance", "isUpToDate"));
		NodesCollection collections = context.getcState().getSenators();
		for (NodeState ns : collections.getNodeStates().values()) {
			String masterDistance = ns.getMasterDistance() + ((ns.getMasterDistance() == 0 && ns.isConnected()) ? "(master)" : "");
			String room = context.getConfiguration().getConfigNodeSet().getSenatorsMap().get(ns.getAddress()).getRoom();
			pw.println(String.format(SENATOR_FORMAT, ns.getAddress(),room, ns.isConnected(), sdf.format(new Date(ns.getLastBeatHeatTime())),
					ns.getLastCanExecuteInstanceId(), masterDistance, ns.isUpToDate()));
		}
		
		pw.println();
		pw.println("followers status:");
		pw.println(String.format(FOLLOWER_FORMAT, "id", "isMasterConnected", "lastHeartTime", "maxLogId", "master-distance", "isUpToDate"));
		for (NodeState ns : context.getcState().getFollowers()) {
			String masterDistance = ns.getMasterDistance() + ((ns.getMasterDistance() == 0 && ns.isConnected()) ? "(master)" : "");
			if(ns.getLastBeatHeatTime() + Constants.MAX_HEART_BEAT_INTERVAL * 2 > TimeUtil.currentTimeMillis()){
				pw.println(String.format(FOLLOWER_FORMAT, ns.getAddress(), ns.isConnected(), sdf.format(new Date(ns.getLastBeatHeatTime())),
						ns.getLastCanExecuteInstanceId(), masterDistance, ns.isUpToDate()));
			}
		}
		return sw.toString();
	}

	@Override
	public int commandLength() {
		return 1;
	}

}