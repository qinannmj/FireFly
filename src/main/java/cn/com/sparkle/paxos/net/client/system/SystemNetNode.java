package cn.com.sparkle.paxos.net.client.system;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.model.ElectionId;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.SuccessTransportConfig;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.model.AddRequest.CommandType;
import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.syncwaitstrategy.SyncStrategyFactory;
import cn.com.sparkle.paxos.net.client.syncwaitstrategy.WaitStrategy;
import cn.com.sparkle.paxos.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.FirstHeartBeatCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.HeartBeatCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.InstanceSucccessCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.ReAssignElectionCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.paxos.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.paxos.net.netlayer.NetCloseException;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.system.callback.CatchUpCallBackV0_0_1;
import cn.com.sparkle.paxos.state.NodeState;

public class SystemNetNode extends NetNode {
	private WaitStrategy syncSuccessfulMessageStrategy;
	private final long INSERT_SUCCESSFUL_MESSAGE_BYTE_SIZE;

	public SystemNetNode(Configuration conf, PaxosSession session, String address, Protocol protocol, String appVersion, int heartBeatnterval) {
		super(session, address, protocol, appVersion, heartBeatnterval);
		this.syncSuccessfulMessageStrategy = SyncStrategyFactory.build(conf);
		INSERT_SUCCESSFUL_MESSAGE_BYTE_SIZE = conf.getSessionSuccessSyncMaxMem() / 4;
	}

	void sendFirstHeartBeat(EventsManager eventsManager) {
		CallBack<? extends Object> callback = getProtocol().createHeartBeatCallBack(new FirstHeartBeatCallBack(eventsManager));
		long packageId = generatePackageId();
		byte[] request = getProtocol().createHeartBeatRequest(packageId);
		try {
			write(request, packageId, callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendHeartBeat(EventsManager eventsManager) throws NetCloseException {
		CallBack<? extends Object> callback = getProtocol().createHeartBeatCallBack(new HeartBeatCallBack(eventsManager));
		long packageId = generatePackageId();
		byte[] request = getProtocol().createHeartBeatRequest(packageId);
		write(request, packageId, callback);
	}

	public void sendElectionPrepareRequest(ElectionId id, long lastVoteId, PrepareCallBack callback) {
		CallBack<? extends Object> _callback = getProtocol().createPaxosPrepareCallBack(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createElectionPrepareRequest(packageId, lastVoteId, id);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendInstancePrepareRequest(Id id, long instanceId, List<String> chain, PrepareCallBack callback) {
		CallBack<? extends Object> _callback = getProtocol().createPaxosPrepareCallBack(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createInstancePrepareRequest(packageId, instanceId, id, chain);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendInstanceVoteRequest(long instanceId, Id id, Value v, List<String> chain, VoteCallBack callback) {
		CallBack<? extends Object> _callback = getProtocol().createPaxosVoteCallBack(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createInstanceVoteRequest(packageId, instanceId, id, v, chain);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendLookUpLatestInstanceIdRequest(LookUpLatestInstanceIdCallBack callback) {
		CallBack<? extends Object> _callback = getProtocol().createLookUpLatestInstanceIdCallBack(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createLookUpLatestInstanceIdRequest(packageId);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	private ReentrantLock successLock = new ReentrantLock();
	private long successfulMessageMayBeHeldUp = 0;

	public void sendInstanceSuccessMessage(long instanceId, Id id, Value value, List<String> notifyList, List<SuccessTransportConfig> notifyChain)
			throws InterruptedException {
		long packageId = -1;
		try {
			successLock.lock();
			if (value != null) {
				for (byte[] bs : value.getValue()) {
					successfulMessageMayBeHeldUp += bs.length;
				}
			}
			CallBack<? extends Object> _callBack = null;
			if (successfulMessageMayBeHeldUp >= INSERT_SUCCESSFUL_MESSAGE_BYTE_SIZE) {
				successfulMessageMayBeHeldUp = 0;
				_callBack = getProtocol().createInstanceSuccessRequestCallBack(new InstanceSucccessCallBack() {
					@Override
					public void receivedResponse() {
						syncSuccessfulMessageStrategy.finishMessageSend();
					}
				});
				syncSuccessfulMessageStrategy.fireStrategy(this.getSession());
				packageId = generatePackageId();
			}
			byte[] request = getProtocol().createInstanceSuccessRequest(packageId, instanceId, id, value, notifyList, notifyChain);
			try {
				write(request, packageId, _callBack);
			} catch (NetCloseException e) {
			}
		} finally {
			successLock.unlock();
		}
	}

	public void sendElectionSuccessMessage(ElectionId id) {
		byte[] request = getProtocol().createElectionSuccessRequest(-1, id);
		try {
			write(request, -1, null);
		} catch (NetCloseException e) {
		}
	}

	public void sendElectionVoteRequest(ElectionId id, long lastVoteId, VoteCallBack callback) {
		CallBack<? extends Object> _callback = getProtocol().createPaxosVoteCallBack(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createElectionVoteRequest(packageId, lastVoteId, id);
		try {
			write(request, packageId, _callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendCatchUpRequest(long instanceId, int size, CatchUpCallBack callback) {
		CallBack<? extends Object> _callBack = new CatchUpCallBackV0_0_1(callback);
		long packageId = generatePackageId();
		byte[] request = getProtocol().createCatchUpRequest(packageId, instanceId, size);
		try {
			write(request, packageId, _callBack);
		} catch (NetCloseException e) {
		}
	}

	public void sendReAssignElectionRequest(String address, ReAssignElectionCallBack callback) {
		long packageId = generatePackageId();
		byte[] request = getProtocol().createReElectionRequest(packageId, address);
		try {
			write(request, packageId, callback);
		} catch (NetCloseException e) {
		}
	}

	public void sendActiveHeartBeat(NodeState nodeState) throws NetCloseException {
		byte[] request = getProtocol().createActiveHeartMessage(nodeState);
		write(request, -1, null);
	}

	public void sendAddRequest(CommandType commandType, byte[] value, AddRequestCallBack callback) throws NetCloseException {
		long packageId = this.generatePackageId();
		byte[] request = getProtocol().createAddRequest(packageId, commandType, value);
		CallBack<? extends Object> _callback = getProtocol().createAddRequestCallBack(callback);
		this.write(request, packageId, _callback);
	}

	@Override
	protected void onClose() {
		super.onClose();
	}

}
