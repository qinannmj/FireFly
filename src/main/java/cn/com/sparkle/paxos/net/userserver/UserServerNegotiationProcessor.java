package cn.com.sparkle.paxos.net.userserver;

import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolManager;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.paxos.protocolprocessor.negotiation.AbstractServerProtocolNegotiationProcessor;

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
