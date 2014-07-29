package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.event.listeners.AccountBookEventListener;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.SenatorHeartBeatResponse;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.state.ClusterState;
import cn.com.sparkle.firefly.state.NodeState;

public class SenatorHeartBeatProcessor extends AbstractProtocolV0_0_1Processor implements CatchUpEventListener, AccountBookEventListener {

	private ClusterState cState = null;

	private volatile boolean isUptodate = false;

	private AccountBook aBook = null;
	
	private Context context = null;

	private volatile boolean accountBookInited = false;

	public SenatorHeartBeatProcessor(Context context) {
		context.getEventsManager().registerListener(this);
		this.aBook = context.getAccountBook();
		this.cState = context.getcState();
		this.context = context;
	}

	@Override
	public void receive(MessagePackage messagePackage, PaxosSession session) throws InterruptedException {
		if (messagePackage.hasHeartBeatRequest()) {
			ElectionId id = cState.getLastElectionId();
			NodesCollection senators = cState.getSenators();
			NodeState masterState = senators.getNodeStates().get(id.getAddress());
			boolean isMasterConnected = masterState == null ? false : masterState.isConnected();

			SenatorHeartBeatResponse.Builder builder = SenatorHeartBeatResponse.newBuilder();
			builder.setIsInited(this.accountBookInited);
			builder.setLastCanExecuteInstanceId(aBook.getLastCanExecutableInstanceId());
			builder.setIsMasterConnected(id.getIncreaseId() < 0 ? false : isMasterConnected);
			builder.setElectionAddress(id.getAddress());
			builder.setElectionId(id.getIncreaseId());
			builder.setElectionVersion(id.getVersion());
			builder.setIsUpToDate(isUptodate);
			builder.setMasterDistance(cState.getMasterDistance());
			builder.setRoom(context.getConfiguration().getRoom());
			for (NetNode node : senators.getValidActiveNodes().values()) {
				builder.addConnectedValidNodes(node.getAddress());
			}
			MessagePackage.Builder response = MessagePackage.newBuilder();
			response.setSenatorHeartBeatResponse(builder);
			response.setId(messagePackage.getId());
			response.setIsLast(true);
			sendResponse(session, response.build().toByteArray());
		} else {
			super.fireOnReceive(messagePackage, session);
		}
	}

	@Override
	public void catchUpFail() {
		isUptodate = false;
	}

	@Override
	public void recoveryFromFail() {
		isUptodate = true;
	}

	@Override
	public void accountInit() {
		accountBookInited = true;
	}

}
