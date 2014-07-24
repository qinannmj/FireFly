package cn.com.sparkle.paxos.protocolprocessor;

import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.net.netlayer.PaxosSessionKeys;

public abstract class AbstractClientReceiveProcessor<T> extends AbstractChainProtocolProcessor<T> {

	public final void receive(PaxosSession session, long id, boolean isLast, T o) {
		NetNode netnode = session.get(PaxosSessionKeys.NET_NODE_KEY);
		netnode.recieve(id, isLast, o);
	}
}
