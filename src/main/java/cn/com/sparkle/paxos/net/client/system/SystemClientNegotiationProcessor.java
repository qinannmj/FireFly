package cn.com.sparkle.paxos.net.client.system;

import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolManager;
import cn.com.sparkle.paxos.protocolprocessor.negotiation.AbstractClientNegotiationProcessor;

public class SystemClientNegotiationProcessor extends AbstractClientNegotiationProcessor {
	private EventsManager eventsManager;

	private Configuration conf;

	public SystemClientNegotiationProcessor(Configuration conf, ProtocolManager protocolManager, EventsManager eventsManager) {
		super(conf.getNetChecksumType(), conf.getHeartBeatInterval(), protocolManager);
		this.eventsManager = eventsManager;
		this.conf = conf;
	}

	@Override
	public NetNode createNetNode(String appVersion, String address, PaxosSession session, Protocol protocol, int heartBeatInterval) {
		SystemNetNode netNode = new SystemNetNode(conf, session, address, protocol, appVersion, heartBeatInterval);
		netNode.sendFirstHeartBeat(eventsManager);
		return netNode;
	}

}
