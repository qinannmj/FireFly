package cn.com.sparkle.paxos.protocolprocessor;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public abstract class AbstractChainProtocolProcessor<T> implements ProtocolProcessor<T> {

	private AbstractChainProtocolProcessor<?> next = null;

	@Override
	public final void fireOnConnect(PaxosSession session) {
		if (next != null) {
			next.onConnect(session);
		}
	}

	@Override
	public final void fireOnDisConnect(PaxosSession session) {
		if (next != null) {
			next.onDisConnect(session);
		}
	}

	@Override
	public final void fireOnReceive(Object o, PaxosSession session) throws InterruptedException {
		if (next != null) {
			next.transformToReceive(o, session);
		}
	}

	@SuppressWarnings("unchecked")
	protected final void transformToReceive(Object o, PaxosSession session) throws InterruptedException {
		receive((T) o, session);
	}

	public AbstractChainProtocolProcessor<?> getNext() {
		return next;
	}

	public void setNext(AbstractChainProtocolProcessor<?> next) {
		this.next = next;
	}

	@Override
	public void onConnect(PaxosSession session) {
		fireOnConnect(session);
	}

	@Override
	public void onDisConnect(PaxosSession session) {
		fireOnDisConnect(session);
	}

}
