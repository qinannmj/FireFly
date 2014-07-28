package cn.com.sparkle.firefly.paxosinstance.paxossender;

import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.paxosinstance.PaxosInstance;

public interface PaxosMessageSender {
	public void sendPrepareRequest(PaxosInstance paxosInstance, long instanceId, Id id);

	public void sendVoteRequest(PaxosInstance paxosInstance, long instanceId, Id id, Value value);

	public void sendSuccessRequest(long instanceId, Id id, Value value);
	
	public String linkInfo();

}
