package cn.com.sparkle.firefly.config;

import java.util.HashMap;
import java.util.HashSet;

public class ConfigNodeSet implements Cloneable {
	private HashSet<ConfigNode> senators = new HashSet<ConfigNode>();
	private HashMap<String, ConfigNode> senatorsMap = new HashMap<String, ConfigNode>();
//	private HashMap<String, HashSet<String>> roomDistributedMap = new HashMap<String, HashSet<String>>();
	private long version;

	public ConfigNodeSet(HashSet<ConfigNode> senators, HashMap<String, ConfigNode> senatorsMap,
			long version) {
		super();
		this.senators = senators;
		this.senatorsMap = senatorsMap;
//		this.roomDistributedMap = roomDistributedMap;
		this.version = version;
	}

	public HashSet<ConfigNode> getSenators() {
		return senators;
	}

	public HashMap<String, ConfigNode> getSenatorsMap() {
		return senatorsMap;
	}


	public long getVersion() {
		return version;
	}

	@Override
	public Object clone() {
		//deeply clone
		HashSet<ConfigNode> newSenators = new HashSet<ConfigNode>();
		HashMap<String, ConfigNode> newSenatorsMap = new HashMap<String, ConfigNode>();
//		HashMap<String, HashSet<String>> newRoomDistributedMap = new HashMap<String, HashSet<String>>();

		for (ConfigNode node : senators) {
			ConfigNode cNode = node.clone();
			newSenators.add(cNode);
			newSenatorsMap.put(cNode.getAddress(), cNode);
		}
		ConfigNodeSet set;
		try {
			set = (ConfigNodeSet) super.clone();
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
