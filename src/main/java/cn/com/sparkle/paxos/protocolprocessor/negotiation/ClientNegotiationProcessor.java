package cn.com.sparkle.paxos.protocolprocessor.negotiation;

import cn.com.sparkle.paxos.net.frame.FrameBody;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolProcessor;

public interface ClientNegotiationProcessor extends ProtocolProcessor<FrameBody> {
	/**
	 * start negotiation process
	 * @param session
	 * @return
	 */
	public void negotiation(PaxosSession session, String nodeAddress , String targetAddress);
}
