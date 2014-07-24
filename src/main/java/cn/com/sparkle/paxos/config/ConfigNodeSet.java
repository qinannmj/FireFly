package cn.com.sparkle.paxos.config;

import java.util.HashMap;
import java.util.HashSet;

public class ConfigNodeSet implements Cloneable {
	private HashSet<ConfigNode> senators = new HashSet<ConfigNode>();
	private HashMap<String, ConfigNode> senatorsMap = new HashMap<String, ConfigNode>();
	private HashMap<String, HashSet<String>> roomDistributedMap = new HashMap<String, HashSet<String>>();
	private long version;

	public ConfigNodeSet(HashSet<ConfigNode> senators, HashMap<String, ConfigNode> senatorsMap, HashMap<String, HashSet<String>> roomDistributedMap,
			long version) {
		super();
		this.senators = senators;
		this.senatorsMap = senatorsMap;
		this.roomDistributedMap = roomDistributedMap;
		this.version = version;
	}

	public HashSet<ConfigNode> getSenators() {
		return senators;
	}

	public HashMap<String, ConfigNode> getSenatorsMap() {
		return senatorsMap;
	}

	protected HashMap<String, HashSet<String>> getRoomDistributedMap() {
		return roomDistributedMap;
	}

	public long getVersion() {
		return version;
	}

	@Override
	public Object clone() {
		//deeply clone
		HashSet<ConfigNode> newSenators = new HashSet<ConfigNode>();
		HashMap<String, ConfigNode> newSenatorsMap = new HashMap<String, ConfigNode>();
		HashMap<String, HashSet<String>> newRoomDistributedMap = new HashMap<String, HashSet<String>>();

		for (ConfigNode node : senators) {
			HashSet<String> room = newRoomDistributedMap.get(node.getRoom());
			if (room == null) {
				room = new HashSet<String>();
				newRoomDistributedMap.put(node.getRoom(), room);
			}

			ConfigNode cNode = node.clone(room);
			newSenators.add(cNode);
			newSenatorsMap.put(cNode.getAddress(), cNode);
		}
		ConfigNodeSet set;
		try {
			set = (ConfigNodeSet) super.clone();
			set.roomDistributedMap = newRoomDistributedMap;
			set.senators = newSenators;
			set.senatorsMap = newSenatorsMap;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return set;
	}

	public void setVersion(long version) {
		this.version = version;
	}

}
