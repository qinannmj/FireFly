package cn.com.sparkle.firefly.paxosinstance.paxossender.builder;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.paxosinstance.paxossender.AbstractInstancePaxosMessageSender;

public interface SenderBuilder {
	public AbstractInstancePaxosMessageSender buildSender(Context context, String type);
}
