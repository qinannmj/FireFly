package cn.com.sparkle.firefly.net.client.system;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.deamon.ReConnectDeamon;
import cn.com.sparkle.firefly.deamon.ReConnectDeamon.ReConnectMethod;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.NodeStateChangeEvent;
import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.filter.FilterProcessorChainBuilder;

public class SystemClientHandler implements NetHandler {
	private final static Logger logger = Logger.getLogger(SystemClientHandler.class);
	private final NetClient netClient;
	private ReentrantLock lock = new ReentrantLock();
	private EventsManager eventsManager;
	private ReConnectDeamon reConnectThread;
	private Configuration conf;

	private ProtocolProcessorChain processor;

	private SystemClientNegotiationProcessor negotiationProcessor;

	public SystemClientHandler(NetClient netClient, EventsManager eventsManager, ReConnectDeamon reConnectThread, Configuration conf,
			ProtocolManager protocolManager) {
		this.netClient = netClient;
		this.eventsManager = eventsManager;
		this.reConnectThread = reConnectThread;
		this.conf = conf;

		negotiationProcessor = new SystemClientNegotiationProcessor(conf, protocolManager, eventsManager);
		processor = new DefaultProtocolProcessorChain();
		processor.addFirst(negotiationProcessor);
		processor.addFirst(FilterProcessorChainBuilder.build());
	}

	@Override
	public void onDisconnect(PaxosSession session) {
		SystemNetNode nnode = (SystemNetNode) session.get(PaxosSessionKeys.NET_NODE_KEY);
		if (nnode != null) {
			nnode.onClose();
			try {
				lock.lock();
				NodeStateChangeEvent.doLoseConnectEvent(eventsManager, nnode);
			} finally {
				lock.unlock();
			}
		}
		ConfigNode node = (ConfigNode) session.get(PaxosSessionKeys.CONFIG_NODE);
		reConnect(node);
		processor.onDisConnect(session);
	}

	@Override
	public void onConnect(PaxosSession session, Object attachment) {
		ConfigNode node = (ConfigNode) attachment;
		session.put(PaxosSessionKeys.CONFIG_NODE, attachment);
		session.put(PaxosSessionKeys.ADDRESS_KEY, node.getAddress()); //just for record the address of this node
		if (node.isValid()) {
			processor.onConnect(session);
			negotiationProcessor.negotiation(session, conf.getSelfAddress(),node.getAddress());
		} else {
			session.closeSession();
		}
	}

	@Override
	public void onRecieve(PaxosSession session, Buf buf) throws InterruptedException {
		processor.receive(buf, session);
	}

	@Override
	public void onRefuse(Object connectAttachment) {
		ConfigNode node = (ConfigNode) connectAttachment;
		reConnect(node);
	}

	private final class ReConnect implements ReConnectMethod {
		private final Logger log = Logger.getLogger(ReConnect.class);

		@Override
		public void reConnect(Object value) {
			ConfigNode node = (ConfigNode) value;
			try {
				if (conf.isDebugLog()) {
					log.debug("reConnect " + node.getAddress() + " " + node.isValid());
				}
				if (node.isValid()) {
					netClient.connect(node.getIp(), Integer.parseInt(node.getPort()), node);
				}
			} catch (Throwable e) {
				SystemClientHandler.this.reConnect(node);
			}
		}

	}

	private final ReConnect method = new ReConnect();

	private void reConnect(ConfigNode node) {
		// After sleep 5 seconds,retry to connect.
		if (node == null) {
			logger.error("node is null");
		}
		reConnectThread.add(node, method, 2000);
	}

}
