package cn.com.sparkle.paxos.net.netlayer;

import cn.com.sparkle.paxos.client.PaxosOperater;
import cn.com.sparkle.paxos.config.ConfigNode;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.client.user.ConnectConfig;
import cn.com.sparkle.paxos.protocolprocessor.negotiation.ServerNegotiationStatus;

public class PaxosSessionKeys {
	public final static AttributeKey<? extends NetNode> NET_NODE_KEY = new AttributeKey<NetNode>("netnode");

	public final static AttributeKey<ServerNegotiationStatus> NEGOTIATION_STATUS_KEY = new AttributeKey<ServerNegotiationStatus>("negotiation_status");

	public final static AttributeKey<String> ADDRESS_KEY = new AttributeKey<String>("address");
	
	public final static AttributeKey<ConfigNode> CONFIG_NODE = new AttributeKey<ConfigNode>("configNode");

	public final static AttributeKey<PaxosOperater> PAXOS_OPERATOR_KEY = new AttributeKey<PaxosOperater>("paxos_operator");

	public final static AttributeKey<ConnectConfig> USER_CLIENT_CONNECT_CONFIG = new AttributeKey<ConnectConfig>("user_client_connect_config");

	//	public final static AttributeKey<Object> JVM_PIPE_ATTACHMENT_KEY = new AttributeKey<Object>("jvm_pipe_attachment");
}
