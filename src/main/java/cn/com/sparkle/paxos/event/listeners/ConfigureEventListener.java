package cn.com.sparkle.paxos.event.listeners;

import java.util.Set;

import cn.com.sparkle.paxos.config.ConfigNode;

public interface ConfigureEventListener extends EventListener {
	public void senatorsChange(Set<ConfigNode> newSenators,ConfigNode addNode,ConfigNode rmNode, long version);
	
	//	public void followersChange(Set<ConfigNode> newFollowers);
}
