package cn.com.sparkle.firefly.config;

import java.util.Set;

public class ConfigNode implements Cloneable {
	private String ip;
	private String port;
	private String address;
	private boolean valid = true;

	public ConfigNode(String ip, String port) {
		super();
		this.ip = ip;
		this.port = port;
		this.address = ip + ":" + port;
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

	//	public boolean isSameRoomNode(String address) {
	//		return sameRoomNode.contains(address);
	//	}
	//
	//	protected HashSet<String> getSameRoomNode() {
	//		return this.sameRoomNode;
	//	}
	//
	//	@SuppressWarnings("unchecked")
	//	public HashSet<String> cloneSameRoomNode() {
	//		return (HashSet<String>) sameRoomNode.clone();
	//	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String toString() {
		return getAddress();
	}

	public void invalid() {
		this.valid = false;
	}

	public boolean isValid() {
		return this.valid;
	}

	public String getAddress() {
		return address;
	}

	public static ConfigNode parseNode(String address) {
		String[] array = address.trim().split(":");
		return new ConfigNode(array[0], array[1]);
	}

	public static boolean exist(Set<ConfigNode> set, String testAddress) {
		ConfigNode node = parseNode(testAddress);
		return set.contains(node);
	}
	@Override
	public ConfigNode clone() {
		try {
			return (ConfigNode)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
