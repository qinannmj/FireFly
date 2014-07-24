package cn.com.sparkle.paxos.protocolprocessor;

public abstract class AbstraceProtocol implements Protocol {
	private ProtocolProcessorChain serverInProcessorChain;
	private ProtocolProcessorChain userInProcessorChain;
	private ProtocolProcessorChain clientReceiveProcessor;

	public AbstraceProtocol(ProtocolProcessorChain serverInProcessorChain, ProtocolProcessorChain userInProcessorChain,
			ProtocolProcessorChain clientReceiveProcessor) {
		super();
		this.serverInProcessorChain = serverInProcessorChain;
		this.userInProcessorChain = userInProcessorChain;
		this.clientReceiveProcessor = clientReceiveProcessor;
	}

	@Override
	public ProtocolProcessorChain getServerInProcessor() {
		return this.serverInProcessorChain;
	}

	@Override
	public ProtocolProcessorChain getUserInProcessor() {
		return this.userInProcessorChain;
	}

	@Override
	public ProtocolProcessorChain getClientReceiveProcessor() {
		return this.clientReceiveProcessor;
	}

	protected void setServerInProcessorChain(ProtocolProcessorChain serverInProcessorChain) {
		this.serverInProcessorChain = serverInProcessorChain;
	}

	protected void setUserInProcessorChain(ProtocolProcessorChain userInProcessorChain) {
		this.userInProcessorChain = userInProcessorChain;
	}

	protected void setClientReceiveProcessor(ProtocolProcessorChain clientReceiveProcessor) {
		this.clientReceiveProcessor = clientReceiveProcessor;
	}

}
