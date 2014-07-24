package cn.com.sparkle.paxos.net.netlayer;

import java.util.HashSet;

public class AttributeKey<T> {
	private String value;
	private static HashSet<String> set = new HashSet<String>();

	public AttributeKey(String value) {
		this.value = value;
		synchronized (AttributeKey.class) {
			if (set.contains(value)) {
				throw new RuntimeException("key[" + this.value + "] is existed!");
			} else {
				set.add(value);
			}
		}
	}
}
