package cn.com.sparkle.firefly.net.userserver;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.AbstractServerProtocolNegotiationProcessor;

public class UserServerNegotiationProcessor extends AbstractServerProtocolNegotiationProcessor {

	public UserServerNegotiationProcessor(ProtocolManager protocolManager, Configuration conf) {
		super(protocolManager, conf);
	}

	@Override
	public ProtocolProcessorChain getChain(String version) {
		Protocol protocol = super.getProtocolManager().getProtocol(version);
		return protocol.getUserInProcessor();
	}

	@Override
	public boolean isAcceptConnect(String targetAddress, String sourceAddress) {
		return true;
	}


}
