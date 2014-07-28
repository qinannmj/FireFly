package cn.com.sparkle.firefly.protocolprocessor.negotiation;

import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessor;

public interface ClientNegotiationProcessor extends ProtocolProcessor<FrameBody> {
	/**
	 * start negotiation process
	 * @param session
	 * @return
	 */
	public void negotiation(PaxosSession session, String nodeAddress , String targetAddress);
}
