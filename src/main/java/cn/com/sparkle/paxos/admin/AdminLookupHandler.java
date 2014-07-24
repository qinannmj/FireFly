package cn.com.sparkle.paxos.admin;

import java.util.HashMap;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.admin.processors.ClusterStateProcessor;
import cn.com.sparkle.paxos.admin.processors.ReElectionProcessor;
import cn.com.sparkle.paxos.admin.processors.StateProcessor;
import cn.com.sparkle.paxos.handlerinterface.HandlerInterface;
import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.model.AddRequest.CommandType;
import cn.com.sparkle.paxos.net.client.system.SystemNetNode;
import cn.com.sparkle.paxos.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.paxos.net.netlayer.NetCloseException;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

/**
 * admin command style :  command nodename  arg1 arg2 arg3 ........
 * @author qinan.qn
 *
 */
public class AdminLookupHandler extends HandlerInterface {
	private final static Logger logger = Logger.getLogger(AdminLookupHandler.class);
	
	private Context context;
	private HashMap<String, AbstractAdminProcessor> processorMap = new HashMap<String, AbstractAdminProcessor>();

	public AdminLookupHandler(Context context) {
		this.context = context;
		ReElectionProcessor electionProcessor= new ReElectionProcessor(context);
		processorMap.put(Commands.RE_ELECTION, electionProcessor);
		processorMap.put(Commands.RE_ELECTION1, electionProcessor);
		
		ClusterStateProcessor clusterStateProcessor = new ClusterStateProcessor(context);
		processorMap.put(Commands.CLSTUER_STATE, clusterStateProcessor);
		processorMap.put(Commands.CLSTUER_STATE1, clusterStateProcessor);
		
		StateProcessor stateProcessor = new StateProcessor(context);
		processorMap.put(Commands.STATE, stateProcessor);
		processorMap.put(Commands.STATE1, stateProcessor);
	}

	@Override
	public void onClientConnect(PaxosSession session) {
	}

	@Override
	public void onClientClose(PaxosSession session) {
	}

	@Override
	public void onReceiveLookUp(final PaxosSession session, final AddRequest request) {
		byte[] b = request.getValue();
		String[] r = (new String(b)).split(" ");
		if (r.length  <2 || context.getConfiguration().getSelfAddress().equals(r[1]) ) {
			//process by self
			AbstractAdminProcessor p = processorMap.get(r[0]);
			logger.info("command: " + new String(b));
			if (p != null) {
				p.process(r, this, session, request);
				context.getcState().getSelfState().addResponseAdminAddRequestCount();
			} else {
				sendResponseCommandResponse(session, request, "Command not found!".getBytes());
			}
		} else {
			//route to target
			SystemNetNode node = context.getcState().getRouteManage().lookupRouteNode(r[1]);
			if (node != null) {
				//route successfully
				try {
					node.sendAddRequest(CommandType.ADMIN_READ, b, new AddRequestCallBack(null, null, null) {
						@Override
						public void fail() {
							AdminLookupHandler.this.sendResponseCommandResponse(session, request, "no route to target!".getBytes());
						}

						@Override
						public void call(byte[] response, boolean isLast) {
							AdminLookupHandler.this.sendResponseCommandResponse(session, request, response);
						}
					});
				} catch (NetCloseException e) {
					//nothing to do
				}
			} else {
				sendResponseCommandResponse(session, request, "no route to target!".getBytes());
			}
		}
	}

	@Override
	public byte[] onLoged(byte[] bytes) {
		//the method will be not invoked!
		throw new RuntimeException("unsupported method!");
	}

}
