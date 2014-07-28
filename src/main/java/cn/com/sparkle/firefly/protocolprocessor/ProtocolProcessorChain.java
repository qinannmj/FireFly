package cn.com.sparkle.firefly.protocolprocessor;

public abstract class ProtocolProcessorChain extends AbstractChainProtocolProcessor<Object> {
	public abstract void addFirst(AbstractChainProtocolProcessor<?> protocolProcessor);

	public abstract void addLast(AbstractChainProtocolProcessor<?> protocolProcessor);
}
