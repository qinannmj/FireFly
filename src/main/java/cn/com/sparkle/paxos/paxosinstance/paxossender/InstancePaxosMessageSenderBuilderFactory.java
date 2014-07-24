package cn.com.sparkle.paxos.paxosinstance.paxossender;

import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.paxosinstance.paxossender.builder.CircleBuilder;
import cn.com.sparkle.paxos.paxosinstance.paxossender.builder.SenderBuilder;
import cn.com.sparkle.paxos.paxosinstance.paxossender.builder.ValidNodeUnChangeBuilder;

public class InstancePaxosMessageSenderBuilderFactory {
	public final static String HALF_CIRCLE = "halfCircleSender";
	public final static String BOARDCAST = "boardcastSender";
	public final static String FULL_CIRCLE = "fullCircleSender";

	public static SenderBuilder getBuilder(Configuration conf) {
		String type = conf.getPaxosSender();
		if (HALF_CIRCLE.equals(type) || FULL_CIRCLE.equals(type)) {
			return new CircleBuilder();
		} else if (BOARDCAST.equals(type)) {
			return new ValidNodeUnChangeBuilder();
		} else {
			throw new RuntimeException("No match sender builder!");
		}
	}
}
