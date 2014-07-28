package cn.com.sparkle.firefly.net.systemserver;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.AbstractServerProtocolNegotiationProcessor;

public class SystemServerProtocolNegotiationProcessor extends AbstractServerProtocolNegotiationProcessor {
	
	private final static Logger logger = Logger.getLogger(SystemServerProtocolNegotiationProcessor.class);
	
	private Configuration conf;
	
	public SystemServerProtocolNegotiationProcessor(ProtocolManager protocolManager, Configuration conf) {
		super(protocolManager, conf);
		this.conf = conf;
	}

	@Override
	public ProtocolProcessorChain getChain(String version) {
		ProtocolManager protocolManager = getProtocolManager();
		Protocol protocol = protocolManager.getProtocol(version);
		return protocol.getServerInProcessor();
	}

	@Override
	public boolean isAcceptConnect(String targetAddress,String sourceAddress) {
		if(conf.getSelfAddress().equals(targetAddress)){
			return true;
		}else{
			logger.info(String.format("Close system connection[from %s]!Cause: the targetAddress is not match the ip of this node!", sourceAddress));
			return false;
		}
	}
}
