package cn.com.sparkle.firefly.protocolprocessor.votevaluetransport;

import java.util.concurrent.ConcurrentHashMap;

import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.Value.ValueType;

public class ValueTransportPipeState {
	private ConcurrentHashMap<Long, InstanceVoteValueState> unFinish = new ConcurrentHashMap<Long, InstanceVoteValueState>();
	
	public final InstanceVoteValueState getState(long messageId){
		return unFinish.get(messageId);
	}
	
	public final void register(long messageId,ValueType valuetype,int dataLength,Object obj){
		InstanceVoteValueState state = new InstanceVoteValueState();
		state.obj = obj;
		state.v = new Value(valuetype, dataLength);
		state.dataLength = dataLength;
		if(unFinish.put(messageId, state) != null){
			throw new RuntimeException("reregister vote transport pipe state");
		}
	}
	
	public final void unRegister(long messageId){
		unFinish.remove(messageId);
	}
	
	public final static class InstanceVoteValueState{
		private Object obj;
		private Value v;
		private int dataLength;
		public Object getObj() {
			return obj;
		}
		public Value getV() {
			return v;
		}
		public int getDataLength() {
			return dataLength;
		}
		
	}
}
