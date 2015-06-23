package cn.com.sparkle.firefly.net.userserver;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.filter.FilterProcessorChainBuilder;

public class UserServerHandler implements NetHandler {

	private final static Logger logger = Logger.getLogger(UserServerHandler.class);

	private HandlerInterface handlerInterface;
	private Configuration conf;

	private ProtocolProcessorChain processor;

	public UserServerHandler(EventsManager eventsManager, final Configuration conf, HandlerInterface handlerInterface, ProtocolManager protocolManager) {
		this.conf = conf;
		this.handlerInterface = handlerInterface;
		UserServerNegotiationProcessor negotiationProcessor = new UserServerNegotiationProcessor(protocolManager, conf);
		processor = new DefaultProtocolProcessorChain();
		processor.addFirst(negotiationProcessor);
		processor.addFirst(FilterProcessorChainBuilder.build());
	}

	@Override
	public void onDisconnect(PaxosSession session) {
		if (logger.isDebugEnabled()) {
			logger.debug("user client disconnect!");
		}
		processor.onDisConnect(session);
		handlerInterface.onClientClose(session);
	}

	@Override
	public void onConnect(PaxosSession session, Object attachment) {
		if (logger.isDebugEnabled()) {
			logger.debug("user client connect!");
		}
		processor.onConnect(session);
		handlerInterface.onClientConnect(session);
	}

	@Override
	public void onRecieve(final PaxosSession session, Buf buffer) throws InterruptedException {
		processor.receive(buffer, session);
	}

	@Override
	public void onRefuse(Object connectAttachment) {
		// this is a server, nothing to do
	}

}
