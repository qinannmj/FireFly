package cn.com.sparkle.firefly.protocolprocessor.v0_0_1;

import java.util.List;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.SuccessTransportConfig;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.net.client.CallBack;
import cn.com.sparkle.firefly.net.client.system.callback.CatchUpCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.HeartBeatCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.InstanceSucccessCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.LookUpLatestInstanceIdCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.PrepareCallBack;
import cn.com.sparkle.firefly.net.client.system.callback.VoteCallBack;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.net.client.user.callback.ConnectRequestCallBack;
import cn.com.sparkle.firefly.protocolprocessor.AbstraceProtocol;
import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ActiveHeartBeatRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.AddRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.AddResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.CatchUpRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.CommandResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ConnectRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionId;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionPrepareRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionSuccessMessage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ElectionVoteRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.HeartBeatRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstancePrepareRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceSuccessMessage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceSuccessTransport;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.InstanceVoteRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.LookUpLatestInstanceIdRequest;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.SenatorHeartBeatResponse;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ValueTrunk;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.ActiveHeartProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.CatchupRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.ElectionPrepareRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.ElectionSuccessRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.ElectionVoteRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstancePrepareRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstanceSuccessRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstanceVoteRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.InstanceVoteValueTransportProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.LookupLastestInstanceIdRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.SenatorHeartBeatProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.CatchUpCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.InstanceSuccessCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.LookUpLatestInstanceIdCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.PrepareCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.VoteCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.AddRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.ConnectRequestProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.UserHeartBeatProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.callback.AddRequestCallBackV0_0_1;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user.callback.ConnectRequestCallBackV0_0_1;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel;
import cn.com.sparkle.firefly.stablestorage.util.IdTranslator;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;
import cn.com.sparkle.firefly.state.NodeState;

import com.google.protobuf.ByteString;

public class ProtocolV0_0_1 extends AbstraceProtocol {

	public static ProtocolV0_0_1 createServerProtocol(Context context, HandlerInterface userHandlerInterface, HandlerInterface adminHandlerInterface) {
		AddRequestProcessor addRequestProcessor=	new AddRequestProcessor(context, userHandlerInterface, adminHandlerInterface);
		
		//initiate server in protocol
		ProtocolProcessorChain serverInprotocolChain = new DefaultProtocolProcessorChain();
		serverInprotocolChain.addFirst(addRequestProcessor);
		serverInprotocolChain.addFirst(new ActiveHeartProcessor(context));
		serverInprotocolChain.addFirst(new LookupLastestInstanceIdRequestProcessor(context));
		serverInprotocolChain.addFirst(new CatchupRequestProcessor(context));
		serverInprotocolChain.addFirst(new ElectionVoteRequestProcessor(context));
		serverInprotocolChain.addFirst(new ElectionSuccessRequestProcessor(context));
		serverInprotocolChain.addFirst(new ElectionPrepareRequestProcessor(context));
		serverInprotocolChain.addFirst(new SenatorHeartBeatProcessor(context));
		serverInprotocolChain.addFirst(new InstancePrepareRequestProcessor(context));
		serverInprotocolChain.addFirst(new InstanceSuccessRequestProcessor(context));
		serverInprotocolChain.addFirst(new InstanceVoteRequestProcessor(context));
		serverInprotocolChain.addFirst(new InstanceVoteValueTransportProcessor(context));
		serverInprotocolChain.addFirst(new ProtocolTransformProcessor());
		

		//initiate user in protocol

		ProtocolProcessorChain userInprotocolChain = new DefaultProtocolProcessorChain();
		userInprotocolChain.addFirst(new ConnectRequestProcessor(context));
		userInprotocolChain.addFirst(new UserHeartBeatProcessor());
		userInprotocolChain.addFirst(addRequestProcessor);
		userInprotocolChain.addFirst(new ProtocolTransformProcessor());

		//initiate client receive protocol

		ProtocolProcessorChain clientInChain = new DefaultProtocolProcessorChain();
		clientInChain.addFirst(new ClientReceiveProcessor());
		clientInChain.addFirst(new ProtocolTransformProcessor());

		return new ProtocolV0_0_1(serverInprotocolChain, userInprotocolChain, clientInChain);
	}

	public static ProtocolV0_0_1 createClientProtocol() {
		ProtocolProcessorChain clientInChain = new DefaultProtocolProcessorChain();
		clientInChain.addFirst(new ClientReceiveProcessor());
		clientInChain.addFirst(new ProtocolTransformProcessor());
		return new ProtocolV0_0_1(null, null, clientInChain);
	}

	public ProtocolV0_0_1(ProtocolProcessorChain serverInProcessorChain, ProtocolProcessorChain userInProcessorChain,
			ProtocolProcessorChain clientReceiveProcessor) {
		super(serverInProcessorChain, userInProcessorChain, clientReceiveProcessor);
	}

	@Override
	public String getVersion() {
		return "0.0.1";
	}

	private MessagePackage.Builder makeMessagePackage(long packageId) {
		return makeMessagePackage(packageId, true);
	}

	private MessagePackage.Builder makeMessagePackage(long packageId, boolean isLast) {
		return MessagePackage.newBuilder().setId(packageId).setIsLast(isLast);
	}

	@Override
	public byte[] createHeartBeatRequest(long packageId) {
		return makeMessagePackage(packageId).setHeartBeatRequest(HeartBeatRequest.getDefaultInstance()).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createHeartBeatCallBack(HeartBeatCallBack callback) {
		return new cn.com.sparkle.firefly.protocolprocessor.v0_0_1.system.callback.HeartBeatCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createElectionPrepareRequest(long packageId, long lastVoteId, cn.com.sparkle.firefly.model.ElectionId id) {
		ElectionPrepareRequest.Builder builder = ElectionPrepareRequest.newBuilder();
		StoreModel.Id.Builder idBuilder = IdTranslator.toStoreModelId(id);
		ElectionId.Builder eid = ElectionId.newBuilder();
		eid.setId(idBuilder);
		eid.setVersion(id.getVersion());
		builder.setId(eid);
		builder.setLastVoteId(lastVoteId);
		return makeMessagePackage(packageId).setElectionPrepareRequest(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createPaxosPrepareCallBack(PrepareCallBack callback) {
		return new PrepareCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createInstancePrepareRequest(long packageId, long instanceId, Id id, List<String> chain) {
		InstancePrepareRequest.Builder builder = InstancePrepareRequest.newBuilder();
		StoreModel.Id.Builder idBuilder = IdTranslator.toStoreModelId(id);
		if (chain != null && chain.size() != 0) {
			builder.addAllChain(chain);
		}
		builder.setId(idBuilder).setInstanceId(instanceId);
		return makeMessagePackage(packageId).setInstancePrepareRequest(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createPaxosVoteCallBack(VoteCallBack callback) {
		return new VoteCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createInstanceVoteRequest(long packageId, long instanceId, Id id,int valueType,int valueLength, List<String> chain) {

		StoreModel.Id.Builder idBuilder = IdTranslator.toStoreModelId(id);

		InstanceVoteRequest.Builder builder = InstanceVoteRequest.newBuilder();
		builder.setInstanceId(instanceId);
		builder.setValuetype(valueType);
		builder.setValueLength(valueLength);
		builder.setVoteId(idBuilder);
		if (chain != null && chain.size() != 0) {
			builder.addAllChain(chain);
		}
		return makeMessagePackage(packageId).setInstanceVoteRequest(builder).build().toByteArray();
	}

	@Override
	public byte[] createLookUpLatestInstanceIdRequest(long packageId) {
		LookUpLatestInstanceIdRequest.Builder builder = LookUpLatestInstanceIdRequest.newBuilder();
		return makeMessagePackage(packageId).setLookUpLatestInstanceIdRequest(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createLookUpLatestInstanceIdCallBack(LookUpLatestInstanceIdCallBack callback) {
		LookUpLatestInstanceIdCallBackV0_0_1 lookupCallback = new LookUpLatestInstanceIdCallBackV0_0_1(callback);
		return lookupCallback;
	}

	@Override
	public byte[] createInstanceSuccessRequest(long packageId, long instanceId, Id id,Value value, List<String> notifyList,
			List<SuccessTransportConfig> notifyChain) {
		InstanceSuccessMessage.Builder builder = InstanceSuccessMessage.newBuilder();
		builder.setId(instanceId);
		StoreModel.Id.Builder sid = IdTranslator.toStoreModelId(id);
		builder.setHighestVoteNum(sid);
		if(value != null){
			builder.setValue(ValueTranslator.toStoreModelValue(value));
		}
		
		if (notifyList != null && notifyList.size() != 0) {
			builder.addAllNotifyAddress(notifyList);
		}
		if (notifyChain != null && notifyChain.size() != 0) {
			for (SuccessTransportConfig config : notifyChain) {
				InstanceSuccessTransport.Builder transport = InstanceSuccessTransport.newBuilder();
				transport.setAddress(config.getTargetAddress());
				transport.setIsTransValue(config.isNotifyTransValue());
				if (config.getTargetNotifyAddress() != null) {
					transport.addAllNotifyAddress(config.getTargetNotifyAddress());
				}
				builder.addNotifyChain(transport);
			}
		}

		return makeMessagePackage(packageId).setInstanceSuccessMessage(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createInstanceSuccessRequestCallBack(InstanceSucccessCallBack callback) {
		return new InstanceSuccessCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createElectionSuccessRequest(long packageId, cn.com.sparkle.firefly.model.ElectionId electionId) {
		ElectionSuccessMessage.Builder builder = ElectionSuccessMessage.newBuilder();
		ElectionId.Builder eid = ElectionId.newBuilder();
		StoreModel.Id.Builder _id = IdTranslator.toStoreModelId(electionId);
		eid.setId(_id).setVersion(electionId.getVersion());
		builder.setId(eid);
		return makeMessagePackage(packageId).setElectionSuccessMessage(builder).build().toByteArray();
	}

	@Override
	public byte[] createElectionVoteRequest(long packageId, long lastVoteId, cn.com.sparkle.firefly.model.ElectionId electionId) {
		ElectionVoteRequest.Builder builder = ElectionVoteRequest.newBuilder();
		ElectionId.Builder eid = ElectionId.newBuilder();
		StoreModel.Id.Builder _id = IdTranslator.toStoreModelId(electionId);
		eid.setId(_id).setVersion(electionId.getVersion());
		builder.setId(eid);
		builder.setLastVoteId(lastVoteId);
		return makeMessagePackage(packageId).setElectionVoteRequest(builder).build().toByteArray();
	}

	@Override
	public byte[] createCatchUpRequest(long packageId, long instanceId, int size) {
		CatchUpRequest.Builder builder = CatchUpRequest.newBuilder();
		builder.setStartInstanceId(instanceId);
		builder.setSize(size);
		return makeMessagePackage(packageId).setCatchUpRequest(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createCatchUpCallback(CatchUpCallBack callback) {
		return new CatchUpCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createAddResponse(long packageId,long instanceId, byte[] bytes, boolean isLast) {
		AddResponse.Builder builder = AddResponse.newBuilder();
		builder.setResult(ByteString.copyFrom(bytes));
		if(instanceId != -1){
			builder.setInstanceId(instanceId);
		}
		return makeMessagePackage(packageId, isLast).setAddResponse(builder).build().toByteArray();
	}

	@Override
	public byte[] createAdminResponse(long packageId, boolean isSuccess, String error) {
		CommandResponse.Builder builder = CommandResponse.newBuilder();
		builder.setIsSuccessful(isSuccess);
		if (error != null) {
			builder.setError(error);
		}
		return makeMessagePackage(packageId).setCommandResponse(builder).build().toByteArray();
	}

	@Override
	public byte[] createConnectRequsetRequest(long packageId, int masterDistance) {
		ConnectRequest.Builder builder = ConnectRequest.newBuilder().setMasterDistance(masterDistance);
		return makeMessagePackage(packageId).setConnectRequest(builder).build().toByteArray();
	}

	@Override
	public byte[] createAddRequest(long packageId, CommandType commandType, byte[] value,long instaceId) {
		AddRequest.Builder builder = AddRequest.newBuilder();
		builder.setValue(ByteString.copyFrom(value));
		builder.setCommandType(commandType.getValue());
		builder.setInstanceId(instaceId);
		return makeMessagePackage(packageId).setAddRequest(builder).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createAddRequestCallBack(AddRequestCallBack callback) {
		return new AddRequestCallBackV0_0_1(callback);
	}


	@Override
	public byte[] createActiveHeartMessage(NodeState nodeState,int lifecycle) {
		SenatorHeartBeatResponse.Builder heart = SenatorHeartBeatResponse.newBuilder();
		heart.addAllConnectedValidNodes(nodeState.getConnectedValidNode());
		heart.setElectionAddress(nodeState.getLastElectionId().getAddress());
		heart.setElectionId(nodeState.getLastElectionId().getIncreaseId());
		heart.setElectionVersion(nodeState.getLastElectionId().getVersion());
		heart.setIsInited(nodeState.isInit());
		heart.setIsMasterConnected(nodeState.isMasterConnected());
		heart.setIsUpToDate(nodeState.isUpToDate());
		heart.setLastCanExecuteInstanceId(nodeState.getLastCanExecuteInstanceId());
		heart.setMasterDistance(nodeState.getMasterDistance());
		heart.setRoom(nodeState.getRoom());

		ActiveHeartBeatRequest.Builder request = ActiveHeartBeatRequest.newBuilder().setAddress(nodeState.getAddress()).setHeartBeatResponse(heart).setLifecycle(lifecycle);

		return makeMessagePackage(-1).setActiveHeartBeatRequest(request).build().toByteArray();
	}

	@Override
	public CallBack<? extends Object> createConnectRequestCallBack(ConnectRequestCallBack callback) {
		return new ConnectRequestCallBackV0_0_1(callback);
	}

	@Override
	public byte[] createValueTrunk(long packageId, byte[] value,int valueOffset,int size) {
		ValueTrunk.Builder vt = ValueTrunk.newBuilder();
		vt.setPart(ByteString.copyFrom(value, valueOffset, size));
		return makeMessagePackage(packageId, size == 0).setValueTrunk(vt).build().toByteArray();
	}

}
