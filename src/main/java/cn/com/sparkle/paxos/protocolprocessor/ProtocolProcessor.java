package cn.com.sparkle.paxos.protocolprocessor;

import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public interface ProtocolProcessor<T> {
	public void receive(T t, PaxosSession session) throws InterruptedException;

	public void onConnect(PaxosSession session);

	public void onDisConnect(PaxosSession session);

	public void fireOnReceive(Object o, PaxosSession session) throws InterruptedException;

	public void fireOnConnect(PaxosSession session);

	public void fireOnDisConnect(PaxosSession session);
}
