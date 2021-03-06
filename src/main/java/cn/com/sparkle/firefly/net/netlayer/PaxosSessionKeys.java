package cn.com.sparkle.firefly.net.netlayer;

import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.ConnectConfig;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.ServerNegotiationStatus;
import cn.com.sparkle.firefly.protocolprocessor.votevaluetransport.ValueTransportPipeState;

public class PaxosSessionKeys {
	public final static AttributeKey<? extends NetNode> NET_NODE_KEY = new AttributeKey<NetNode>("netnode");

	public final static AttributeKey<ServerNegotiationStatus> NEGOTIATION_STATUS_KEY = new AttributeKey<ServerNegotiationStatus>("negotiation_status");

	public final static AttributeKey<String> ADDRESS_KEY = new AttributeKey<String>("address");
	
	public final static AttributeKey<ConfigNode> CONFIG_NODE = new AttributeKey<ConfigNode>("configNode");

	public final static AttributeKey<PaxosOperater> PAXOS_OPERATOR_KEY = new AttributeKey<PaxosOperater>("paxos_operator");

	public final static AttributeKey<ConnectConfig> USER_CLIENT_CONNECT_CONFIG = new AttributeKey<ConnectConfig>("user_client_connect_config");
	
	public final static AttributeKey<ValueTransportPipeState> VOTE_VAlUE_TRANSPORT_PIPE_STATE = new AttributeKey<ValueTransportPipeState>("vote_value_pipe");
}
