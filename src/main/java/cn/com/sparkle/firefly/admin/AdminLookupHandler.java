package cn.com.sparkle.firefly.admin;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.admin.processors.AdminProcessor;
import cn.com.sparkle.firefly.admin.processors.ChangeRoomProcessor;
import cn.com.sparkle.firefly.admin.processors.ClusterStateProcessor;
import cn.com.sparkle.firefly.admin.processors.ReElectionProcessor;
import cn.com.sparkle.firefly.admin.processors.StateProcessor;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.net.client.system.SystemNetNode;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

/**
 * admin command style :  command nodename  arg1 arg2 arg3 ........
 * @author qinan.qn
 *
 */
public class AdminLookupHandler extends HandlerInterface {
	private final static Logger logger = Logger.getLogger(AdminLookupHandler.class);

	private Context context;
	private HashMap<String, AdminProcessor> processorMap = new HashMap<String, AdminProcessor>();

	public AdminLookupHandler(Context context, List<AdminProcessor> processors) {
		this.context = context;

		ReElectionProcessor electionProcessor = new ReElectionProcessor(context);
		ClusterStateProcessor clusterStateProcessor = new ClusterStateProcessor(context);
		StateProcessor stateProcessor = new StateProcessor(context);
		ChangeRoomProcessor changeRoomProcessor = new ChangeRoomProcessor(context);

		processors.add(electionProcessor);
		processors.add(clusterStateProcessor);
		processors.add(stateProcessor);
		processors.add(changeRoomProcessor);

		//load into command map
		for (AdminProcessor processor : processors) {
			for (String name : processor.getName()) {
				if (processorMap.put(name, processor) != null) {
					throw new RuntimeException(String.format("repeat name of adminprocessor,name:%s", name));
				}
			}
		}
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
		if (r.length < 2 || context.getConfiguration().getSelfAddress().equals(r[1])) {
			//process by self
			AdminProcessor p = processorMap.get(r[0]);
			if (logger.isDebugEnabled()) {
				logger.info("command: " + new String(b));
			}
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
					node.sendAddRequest(CommandType.ADMIN_READ, -1, b, new AddRequestCallBack(null, null, null) {
						@Override
						public void fail() {
							AdminLookupHandler.this.sendResponseCommandResponse(session, request, "no route to target!".getBytes());
						}

						@Override
						public void call(byte[] response, long instanceId, boolean isLast) {
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
	public byte[] onLoged(byte[] bytes, int offset, int length) {
		//the method will be not invoked!
		throw new RuntimeException("unsupported method!");
	}

	@Override
	public void onInstanceIdExecuted(long instanceId) {
		//nothing to do
	}

}
