package cn.com.sparkle.firefly.net.userserver;

import java.io.PrintWriter;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.AbstractServerProtocolNegotiationProcessor;

public class UserServerNegotiationProcessor extends AbstractServerProtocolNegotiationProcessor {
	private Configuration conf;
	public UserServerNegotiationProcessor(ProtocolManager protocolManager, Configuration conf) {
		super(protocolManager, conf);
		this.conf = conf;
	}

	@Override
	public ProtocolProcessorChain getChain(String version) {
		Protocol protocol = super.getProtocolManager().getProtocol(version);
		return protocol.getUserInProcessor();
	}

	@Override
	public String isAcceptConnect(String targetAddress, String sourceAddress) {
		if(conf.isArbitrator()){
			return Constants.ERROR_NEGOTIATE_ARBITRATOR;
		}else{
			return null;
		}
	}

	@Override
	protected void writeCustomParam(PrintWriter pw) {
	}

}
