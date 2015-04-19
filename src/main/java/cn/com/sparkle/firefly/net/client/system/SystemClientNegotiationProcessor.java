package cn.com.sparkle.firefly.net.client.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.AbstractClientNegotiationProcessor;

public class SystemClientNegotiationProcessor extends AbstractClientNegotiationProcessor {
	private EventsManager eventsManager;

	private Configuration conf;

	public SystemClientNegotiationProcessor(Configuration conf, ProtocolManager protocolManager, EventsManager eventsManager) {
		super(conf.getNetChecksumType(), conf.getHeartBeatInterval(), protocolManager);
		this.eventsManager = eventsManager;
		this.conf = conf;
	}

	@Override
	public NetNode createNetNode(String appVersion, String address,List<String> customParam, PaxosSession session, Protocol protocol, int heartBeatInterval) {
		boolean isArbitrator = false;
		if(customParam.size() > 1){
			isArbitrator = Boolean.parseBoolean(customParam.get(1));
		}
		SystemNetNode netNode = new SystemNetNode(conf, session, address,Integer.parseInt(customParam.get(0)), protocol, appVersion, heartBeatInterval,isArbitrator);
		netNode.sendFirstHeartBeat(eventsManager);
		return netNode;
	}

	@Override
	protected List<String> readCustomParam(BufferedReader br) throws IOException {
		ArrayList<String> list = new ArrayList<String>();
		String line = null;
		//read user port
		//read isArbitrator
		while((line = br.readLine()) != null){
			list.add(line);
		}
		
		return list;
	}

}
