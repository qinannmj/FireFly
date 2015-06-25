package cn.com.sparkle.firefly.protocolprocessor.v0_0_1.user;

import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class UserHeartBeatProcessor extends AbstractProtocolV0_0_1Processor {
	//	private final static Logger logger = Logger.getLogger(UserHeartBeatProcessor.class);

	@Override
	public void receive(MessagePackage t, PaxosSession session) throws InterruptedException {
		if (t.hasHeartBeatRequest()) {
			sendResponse(session, t);
		} else {
			super.fireOnReceive(t, session);
		}
	}

}
