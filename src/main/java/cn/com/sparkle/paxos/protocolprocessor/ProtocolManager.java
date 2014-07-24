package cn.com.sparkle.paxos.protocolprocessor;

import java.util.HashMap;
import java.util.Set;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.handlerinterface.HandlerInterface;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.ProtocolV0_0_1;

public class ProtocolManager {
	private HashMap<String, Protocol> protocolMap = new HashMap<String, Protocol>();

	private ProtocolManager() {
	}

	public static ProtocolManager createServerProtocol(Context context, HandlerInterface userHandlerInterface, HandlerInterface adminHandlerInterface) {
		ProtocolManager pm = new ProtocolManager();
		context.setProtocolManager(pm);
		ProtocolV0_0_1 v0_0_1 = ProtocolV0_0_1.createServerProtocol(context, userHandlerInterface, adminHandlerInterface);
		pm.register(v0_0_1.getVersion(), v0_0_1);

		return pm;
	}

	public static ProtocolManager createClientProtocolManager() {
		ProtocolManager pm = new ProtocolManager();
		ProtocolV0_0_1 v0_0_1 = ProtocolV0_0_1.createClientProtocol();
		pm.register(v0_0_1.getVersion(), v0_0_1);
		return pm;
	}

	public Protocol getProtocol(String version) {
		return protocolMap.get(version);
	}

	public void register(String version, Protocol protocol) {
		this.protocolMap.put(version, protocol);
	}

	public Set<String> getCompatibleProtocolVersion() {
		return protocolMap.keySet();
	}
}