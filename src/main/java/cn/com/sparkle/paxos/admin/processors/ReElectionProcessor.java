package cn.com.sparkle.paxos.admin.processors;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.admin.AbstractAdminProcessor;
import cn.com.sparkle.paxos.admin.AdminLookupHandler;
import cn.com.sparkle.paxos.event.events.ElectionEvent;
import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

/**
 * target command targetElectionIp
 * @author qinan.qn
 *
 */
public class ReElectionProcessor extends AbstractAdminProcessor {
	private Context context;

	public ReElectionProcessor(Context context) {
		this.context = context;
	}

	@Override
	public String processComm(String[] command, AdminLookupHandler handler, PaxosSession session, AddRequest request) {
		if (command[1].equals(context.getConfiguration().getSelfAddress())) {
			if(context.getConfiguration().isElectSelfMaster()){
				ElectionEvent.doReElection(context.getEventsManager());
				return "try to elect " + command[1] + " to master!";
			}else{
				return "This node has configured to forbid itself to master!";
			}
		} else {
			return "The nodeId is not belonged to this node!";
		}
	}

	@Override
	public int commandLength() {
		return 2;
	}

}
