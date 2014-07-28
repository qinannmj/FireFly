package cn.com.sparkle.firefly.protocolprocessor.v0_0_1;

import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.AbstractClientReceiveProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class ClientReceiveProcessor extends AbstractClientReceiveProcessor<MessagePackage> {
	@Override
	public void receive(MessagePackage t, PaxosSession session) {
		receive(session, t.getId(), t.getIsLast(), t);
	}
}
