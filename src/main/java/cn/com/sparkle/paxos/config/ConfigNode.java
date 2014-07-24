package cn.com.sparkle.paxos.config;

import java.util.HashSet;
import java.util.Set;

public class ConfigNode implements Cloneable {
	private String ip;
	private String port;
	private String clientPort;
	private String address;
	private String room;
	private HashSet<String> sameRoomNode;
	private boolean valid = true;

	public ConfigNode(String ip, String port, String clientPort, String room, HashSet<String> sameRoomNode) {
		super();
		this.ip = ip;
		this.port = port;
		this.clientPort = clientPort;
		this.address = ip + ":" + port;
		this.sameRoomNode = sameRoomNode;
		this.room = room;
		if (sameRoomNode != null) {
			sameRoomNode.add(this.address);
		}
	}

	@Override
	public int hashCode() {
		return (ip + ":" + port).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConfigNode) {
			ConfigNode s = (ConfigNode) obj;
			return ip.equals(s.ip) && port.endsWith(s.port);
		}
		return false;
	}

	public boolean isSameRoomNode(String address) {
		return sameRoomNode.contains(address);
	}

	protected HashSet<String> getSameRoomNode() {
		return this.sameRoomNode;
	}

	@SuppressWarnings("unchecked")
	public HashSet<String> cloneSameRoomNode() {
		return (HashSet<String>) sameRoomNode.clone();
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getClientPort() {
		return clientPort;
	}

	public void setClientPort(String clientPort) {
		this.clientPort = clientPort;
	}

	public String toString() {
		return getFullAddress();
	}

	public String getRoom() {
		return room;
	}
	public void invalid(){
		this.valid = false;
	}
	public boolean isValid(){
		return this.valid;
	}

	public String getFullAddress() {
		return ip + ":" + port + ":" + clientPort;
	}

	public String getAddress() {
		return address;
	}

	public static ConfigNode parseNode(String address, String room, HashSet<String> sameHashSet) {
		String[] array = address.trim().split(":");
		return new ConfigNode(array[0], array[1], array.length == 3 ? array[2] : "", room, sameHashSet);
	}

	public static boolean exist(Set<ConfigNode> set, String testAddress) {
		ConfigNode node = parseNode(testAddress, null, null);
		return set.contains(node);
	}

	public ConfigNode clone(HashSet<String> sameHashSet) {
		try {
			ConfigNode cNode = (ConfigNode) clone();
			cNode.sameRoomNode = sameHashSet;
			sameHashSet.add(cNode.getAddress());
			return cNode;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

	}
}
