package cn.com.sparkle.firefly.protocolprocessor.filter;

import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;

public class FilterProcessorChainBuilder {
	public static ProtocolProcessorChain build() {
		ProtocolProcessorChain chain = new DefaultProtocolProcessorChain();
		chain.addFirst(new ConnectCheckFilter());
		chain.addFirst(new FrameUnpackFilter());
		return chain;
	}

}
