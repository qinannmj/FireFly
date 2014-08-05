package cn.com.sparkle.firefly.net.client.system;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.model.ElectionId;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.SuccessTransportConfig;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.syncwaitstrategy.SyncStrategyFactory;
import cn.com.sparkle.firefly.net.client.syncwaitstrategy.WaitStrategy;
import cn.com.sparkle.firefly.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.FirstHeartBeatCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.HeartBeatCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.InstanceSucccessCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.CatchUpCallBackV0_0_1;
import cn.com.sparkle.firefly.state.NodeState;

public class SystemNetNode extends NetNode {
	private WaitStrategy syncSuccessfulMessageStrategy;
	private final long INSERT_SUCCESSFUL_MESSAGE_BYTE_SIZE;

	private int userPort;
	
	private Configuration conf;

	public SystemNetNode(Configuration conf, PaxosSession session, String address, int userPort, Protocol protocol, String appVersion, int heartBeatnterval) {
		super(session, address, protocol, appVersion, heartBeatnterval);
		this.syncSuccessfulMessageStrategy = SyncStrategyFactory.build(conf);
		INSERT_SUCCESSFUL_MESSAGE_BYTE_SIZE = conf.getSessionSuccessSyncMaxMem() / 4;
		this.userPort = userPort;
		this.conf = conf;
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
		
		long messageId = sendInstanceVoteRequest(instanceId, id,v.getValueType().getValue(),v.length(), chain);
		//send value trunk
		int totalSize = v.length();
		int trunksize = (int)Math.ceil( (double)totalSize / conf.getVoteValueSplitSize());
		int offset = 0;
		int remained = totalSize;
		for(int i = 0 ; i < trunksize-1 ; ++i ){
			sendVoteValueTrunk(messageId, null, v.getValuebytes(),offset , conf.getVoteValueSplitSize());
			offset += conf.getVoteValueSplitSize();
			remained -= conf.getVoteValueSplitSize();
		}
		sendVoteValueTrunk(messageId, callback, v.getValuebytes(),offset , remained);
	}

	public long sendInstanceVoteRequest(long instanceId, Id id,int valueType,int valueLength, List<String> chain) {
		long packageId = generatePackageId();
		byte[] request = getProtocol().createInstanceVoteRequest(packageId, instanceId, id,valueType,valueLength, chain);
		try {
			write(request, packageId, null);
		} catch (NetCloseException e) {
		}
		return packageId;
	}

	public void sendVoteValueTrunk(long packageId,VoteCallBack callback,byte[] bytes,int offset,int size){
		byte[] request = getProtocol().createValueTrunk(packageId, bytes, offset, size);
		CallBack<? extends Object> _callback = null;
		if(callback != null){
			_callback = getProtocol().createPaxosVoteCallBack(callback);
		}
		try{
			write(request,packageId,_callback);
		}catch(NetCloseException e){
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
				successfulMessageMayBeHeldUp += value.length();
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

	public void sendActiveHeartBeat(NodeState nodeState) throws NetCloseException {
		byte[] request = getProtocol().createActiveHeartMessage(nodeState);
		write(request, -1, null);
	}

	public void sendAddRequest(CommandType commandType, long instanceId, byte[] value, AddRequestCallBack callback) throws NetCloseException {
		long packageId = this.generatePackageId();
		byte[] request = getProtocol().createAddRequest(packageId, commandType, value, instanceId);
		CallBack<? extends Object> _callback = getProtocol().createAddRequestCallBack(callback);
		this.write(request, packageId, _callback);
	}

	@Override
	protected void onClose() {
		super.onClose();
	}

	public int getUserPort() {
		return userPort;
	}

}
