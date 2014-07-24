package cn.com.sparkle.paxos.protocolprocessor;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public class DefaultProtocolProcessorChain extends ProtocolProcessorChain {
	private Logger logger = Logger.getLogger(DefaultProtocolProcessorChain.class);

	AbstractChainProtocolProcessor<?> first = new AbstractChainProtocolProcessor<Object>() {
		@Override
		public void receive(Object t, PaxosSession session) throws InterruptedException {
			super.fireOnReceive(t, session);
		}
	};
	AbstractChainProtocolProcessor<?> end = first;

	@Override
	public void addFirst(AbstractChainProtocolProcessor<?> protocolProcessor) {
		protocolProcessor.setNext(first.getNext());
		first.setNext(protocolProcessor);
		if (end == first) {
			end = protocolProcessor;
		}
	}

	@Override
	public void addLast(AbstractChainProtocolProcessor<?> protocolProcessor) {
		end.setNext(protocolProcessor);
		end = protocolProcessor;
	}

	@Override
	public void setNext(AbstractChainProtocolProcessor<?> next) {
		addLast(next);
	}

	@Override
	public AbstractChainProtocolProcessor<?> getNext() {
		return end.getNext();
	}

	@Override
	public void receive(Object t, PaxosSession session) throws InterruptedException {
		try {
			first.transformToReceive(t, session);
		} catch (RuntimeException e) {
			session.closeSession();
			logger.error("", e);
			throw e;
		}
	}

	@Override
	public void onConnect(PaxosSession session) {
		first.onConnect(session);
	}

	@Override
	public void onDisConnect(PaxosSession session) {
		first.onDisConnect(session);
	}
}
