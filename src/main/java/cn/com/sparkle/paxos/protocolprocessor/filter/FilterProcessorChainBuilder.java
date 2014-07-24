package cn.com.sparkle.paxos.protocolprocessor.filter;

import cn.com.sparkle.paxos.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolProcessorChain;

public class FilterProcessorChainBuilder {
	public static ProtocolProcessorChain build() {
		ProtocolProcessorChain chain = new DefaultProtocolProcessorChain();
		chain.addFirst(new ConnectCheckFilter());
		chain.addFirst(new FrameUnpackFilter());
		return chain;
	}

}
