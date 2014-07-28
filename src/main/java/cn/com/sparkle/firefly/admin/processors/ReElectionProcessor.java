package cn.com.sparkle.firefly.admin.processors;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.admin.AbstractAdminProcessor;
import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.event.events.ElectionEvent;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

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
