package cn.com.sparkle.paxos.protocolprocessor;

import java.util.List;

import cn.com.sparkle.paxos.model.ElectionId;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.SuccessTransportConfig;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.model.AddRequest.CommandType;
import cn.com.sparkle.paxos.net.client.CallBack;
import cn.com.sparkle.paxos.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.HeartBeatCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.InstanceSucccessCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.paxos.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.paxos.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.paxos.net.client.user.callback.ConnectRequestCallBack;
import cn.com.sparkle.paxos.state.NodeState;

public interface Protocol {
	public String getVersion();

	public ProtocolProcessorChain getServerInProcessor();

	public ProtocolProcessorChain getUserInProcessor();

	public ProtocolProcessorChain getClientReceiveProcessor();

	public CallBack<? extends Object> createHeartBeatCallBack(HeartBeatCallBack callback);

	public CallBack<? extends Object> createPaxosPrepareCallBack(PrepareCallBack callback);

	public CallBack<? extends Object> createPaxosVoteCallBack(VoteCallBack callback);

	public CallBack<? extends Object> createLookUpLatestInstanceIdCallBack(LookUpLatestInstanceIdCallBack callback);

	public CallBack<? extends Object> createInstanceSuccessRequestCallBack(InstanceSucccessCallBack callback);

	public CallBack<? extends Object> createCatchUpCallback(CatchUpCallBack callback);

	public CallBack<? extends Object> createAddRequestCallBack(AddRequestCallBack callback);

	public CallBack<? extends Object> createConnectRequestCallBack(ConnectRequestCallBack callback);

	public byte[] createHeartBeatRequest(long packageId);

	public byte[] createElectionPrepareRequest(long packageId, long lastVoteId, ElectionId id);

	public byte[] createElectionVoteRequest(long packageId, long lastVoteId, ElectionId id);

	public byte[] createInstancePrepareRequest(long packageId, long instanceId, Id id, List<String> chain);

	public byte[] createInstanceVoteRequest(long packageId, long instanceId, Id id, Value v, List<String> chain);

	public byte[] createLookUpLatestInstanceIdRequest(long packageId);

	public byte[] createInstanceSuccessRequest(long packageId, long instanceId, Id id, Value value, List<String> notifyList,
			List<SuccessTransportConfig> notifyChain);

	public byte[] createElectionSuccessRequest(long packageId, ElectionId electionId);

	public byte[] createCatchUpRequest(long packageId, long instanceId, int size);

	public byte[] createAddResponse(long packageId, byte[] bytes, boolean isLast);

	public byte[] createAdminResponse(long packageId, boolean isSuccess, String error);

	public byte[] createConnectRequsetRequest(long packageId, int masterDistance);

	public byte[] createAddRequest(long packageId, CommandType commandType, byte[] value);

	public byte[] createReElectionRequest(long packageId, String address);

	public byte[] createActiveHeartMessage(NodeState nodeState);

}
