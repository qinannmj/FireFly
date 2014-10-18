package cn.com.sparkle.firefly.net.netlayer.raptor;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;

public class RaptorHandler implements IoHandler {
	private final static Logger logger = Logger.getLogger(RaptorHandler.class);
	public NetHandler netHandler;

	public RaptorHandler(NetHandler netHandler) {
		this.netHandler = netHandler;
	}

	@Override
	public void onSessionOpened(IoSession session) {
		RaptorPaxosSession rps = new RaptorPaxosSession(session);
		Object o = session.attachment();
		session.attach(rps);
		netHandler.onConnect(rps, o);
	}

	@Override
	public void onSessionClose(IoSession session) {
		RaptorPaxosSession rps = (RaptorPaxosSession) session.attachment();
		netHandler.onDisconnect(rps);
	}

	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
		RaptorPaxosSession rps = (RaptorPaxosSession) session.attachment();
		try {
			netHandler.onRecieve(rps, (Buf) message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onMessageSent(IoSession session, int sendSize) {
	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		if (!(session.attachment() instanceof RaptorPaxosSession)) {
			netHandler.onRefuse(session.attachment());
		}else{
			logger.error(session.getLocalAddress() + "  " + session.getRemoteAddress() , e);
		}
		
	}

}
