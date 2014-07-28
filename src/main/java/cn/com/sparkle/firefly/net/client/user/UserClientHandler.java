package cn.com.sparkle.firefly.net.client.user;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.deamon.ReConnectDeamon;
import cn.com.sparkle.firefly.deamon.ReConnectDeamon.ReConnectMethod;
import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.filter.FilterProcessorChainBuilder;

public class UserClientHandler implements NetHandler {

	private final static Logger logger = Logger.getLogger(UserClientHandler.class);

	private NetClient client;
	private ReConnectDeamon reConnectThread;
	private boolean debugLog;

	private ProtocolProcessorChain processor;
	private UserClientNegotiationProcessor negotiationProcessor;

	public UserClientHandler(NetClient client, int preferChecksumType, int heartBeatInterval, ProtocolManager protocolManager, ReConnectDeamon reConnectThread,
			boolean debugLog) {
		this.client = client;
		this.reConnectThread = reConnectThread;
		this.debugLog = debugLog;

		negotiationProcessor = new UserClientNegotiationProcessor(preferChecksumType, heartBeatInterval, protocolManager);
		processor = new DefaultProtocolProcessorChain();
		processor.addFirst(negotiationProcessor);
		processor.addFirst(FilterProcessorChainBuilder.build());
	}

	@Override
	public void onDisconnect(PaxosSession session) {

		ConnectConfig connectConfig = (ConnectConfig) session.get(PaxosSessionKeys.USER_CLIENT_CONNECT_CONFIG);
		UserNetNode node = (UserNetNode) session.get(PaxosSessionKeys.NET_NODE_KEY);
		if(node != null){
			node.onClose();
			connectConfig.disconnected(node);
		}
		
		reConnect(connectConfig);
		processor.onDisConnect(session);

	}

	@Override
	public void onConnect(PaxosSession session, Object connectAttachment) {

		ConnectConfig connectConfig = (ConnectConfig) connectAttachment;
		session.put(PaxosSessionKeys.USER_CLIENT_CONNECT_CONFIG, connectConfig);
		session.put(PaxosSessionKeys.ADDRESS_KEY, connectConfig.getAddress()); //just for record the address of this node
		if (debugLog) {
			logger.debug("connected " + connectConfig.getAddress());
		}
		negotiationProcessor.negotiation(session, "user-client",connectConfig.getAddress());
		processor.onConnect(session);
	}

	@Override
	public void onRecieve(PaxosSession session, Buf buf) throws InterruptedException {
		processor.receive(buf, session);
	}

	@Override
	public void onRefuse(Object connectAttachment) {
		reConnect((ConnectConfig) connectAttachment);

	}

	private final class ReConnect implements ReConnectMethod {
		private final Logger log = Logger.getLogger(ReConnect.class);

		@Override
		public void reConnect(Object value) {
			ConnectConfig connectConfig = (ConnectConfig) value;
			String[] a = connectConfig.getAddress().split(":");
			try {
				if (debugLog) {
					log.debug("reConnect " + connectConfig.getAddress());
				}
				client.connect(a[0], Integer.parseInt(a[1]), connectConfig);
			} catch (Throwable e) {
				reConnectThread.add(connectConfig, this, 2000);
			}
		}

	}

	private final ReConnect method = new ReConnect();

	private void reConnect(ConnectConfig connectConfig) {
		// After sleep 5 seconds,retry to connect.
		if (connectConfig.isAutoReConnect()) {
			reConnectThread.add(connectConfig, method, 5000);
		}
	}

}
