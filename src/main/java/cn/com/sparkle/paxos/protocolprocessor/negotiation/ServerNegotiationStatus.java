package cn.com.sparkle.paxos.protocolprocessor.negotiation;

import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolProcessorChain;

public class ServerNegotiationStatus {
	private String appVersion;
	private ProtocolProcessorChain chain;
	private Protocol protocol;

	public ServerNegotiationStatus(String appVersion, ProtocolProcessorChain chain, Protocol protocol) {
		super();
		this.appVersion = appVersion;
		this.chain = chain;
		this.protocol = protocol;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public ProtocolProcessorChain getChain() {
		return chain;
	}

	public Protocol getProtocol() {
		return protocol;
	}

}
