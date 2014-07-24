package cn.com.sparkle.paxos.paxosinstance.paxossender;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.paxosinstance.PaxosInstance;

public interface PaxosMessageSender {
	public void sendPrepareRequest(PaxosInstance paxosInstance, long instanceId, Id id);

	public void sendVoteRequest(PaxosInstance paxosInstance, long instanceId, Id id, Value value);

	public void sendSuccessRequest(long instanceId, Id id, Value value);
	
	public String linkInfo();

}
