package cn.com.sparkle.firefly.net.systemserver;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.CatchUpEventListener;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.protocolprocessor.DefaultProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;
import cn.com.sparkle.firefly.protocolprocessor.filter.FilterProcessorChainBuilder;

public class SystemServerHandler implements NetHandler, CatchUpEventListener {
	private final static Logger logger = Logger.getLogger(SystemServerHandler.class);

	@SuppressWarnings("unused")
	private EventsManager eventsManager;
	private ReentrantLock connectLock = new ReentrantLock();
	private HashSet<PaxosSession> activeSession = new HashSet<PaxosSession>();
	private boolean connectAble = true;
	private volatile ProtocolProcessorChain processor = null;
	private Configuration conf;

	public SystemServerHandler(EventsManager eventsManager, Configuration conf) {
		this.eventsManager = eventsManager;
		this.conf = conf;
		eventsManager.registerListener(this);

	}

	public void initProtocolManager(ProtocolManager protocolManager) {
		SystemServerProtocolNegotiationProcessor negotiationProcessor = new SystemServerProtocolNegotiationProcessor(protocolManager, conf);
		processor = new DefaultProtocolProcessorChain();
		processor.addFirst(negotiationProcessor);
		processor.addFirst(FilterProcessorChainBuilder.build());
	}

	@Override
	public void onConnect(PaxosSession session, Object connectAttachment) {
		try {
			connectLock.lock();
			if (connectAble) {
				activeSession.add(session);
			} else {
				logger.info("system has not finished init procedure");
				session.closeSession();
			}
		} finally {
			connectLock.unlock();
		}
	}

	@Override
	public void onDisconnect(PaxosSession session) {
		try {
			connectLock.lock();
			activeSession.remove(session);
		} finally {
			connectLock.unlock();
		}
	}

	@Override
	public void onRecieve(final PaxosSession session, final Buf buf) throws InterruptedException {
		while (processor == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		processor.receive(buf, session);
	}

	@Override
	public void onRefuse(Object connectAttachment) {
		// this is a server nothing to do
	}

	@Override
	public void catchUpFail() {
		try {
			connectLock.lock();
			logger.debug("catch up fail,and close all connection from others of senator");
			connectAble = false;
			// close all connection from others
			for (PaxosSession session : activeSession) {
				session.closeSession();
			}
			activeSession.clear();
		} finally {
			connectLock.unlock();
		}
	}

	@Override
	public void recoveryFromFail() {
		try {
			connectLock.lock();
			connectAble = true;
		} finally {
			connectLock.unlock();
		}
	}
}
