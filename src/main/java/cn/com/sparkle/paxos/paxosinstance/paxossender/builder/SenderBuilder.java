package cn.com.sparkle.paxos.paxosinstance.paxossender.builder;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.paxosinstance.paxossender.AbstractInstancePaxosMessageSender;

public interface SenderBuilder {
	public AbstractInstancePaxosMessageSender buildSender(Context context, String type);
}
