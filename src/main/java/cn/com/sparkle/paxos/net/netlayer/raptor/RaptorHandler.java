package cn.com.sparkle.paxos.net.netlayer.raptor;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.net.netlayer.NetHandler;
import cn.com.sparkle.paxos.net.netlayer.buf.Buf;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;

public class RaptorHandler implements ProtocolHandler {
	private final static Logger logger = Logger.getLogger(RaptorHandler.class);
	public NetHandler netHandler;

	public RaptorHandler(NetHandler netHandler) {
		this.netHandler = netHandler;
	}

	@Override
	public void onOneThreadSessionOpen(ProtocolHandlerIoSession session) {
		RaptorPaxosSession rps = new RaptorPaxosSession(session);
		Object o = session.customAttachment;
		session.customAttachment = rps;
		netHandler.onConnect(rps, o);
	}

	@Override
	public void onOneThreadSessionClose(ProtocolHandlerIoSession session) {
		RaptorPaxosSession rps = (RaptorPaxosSession) session.customAttachment;
		netHandler.onDisconnect(rps);
	}

	@Override
	public void onOneThreadCatchException(IoSession ioSession, ProtocolHandlerIoSession attachment, Throwable e) {
		if (attachment == null) {
			netHandler.onRefuse(ioSession.attachment());
		}
		logger.error("", e);
	}

	@Override
	public void onOneThreadMessageRecieved(Object receiveObject, ProtocolHandlerIoSession session) throws InterruptedException {
		RaptorPaxosSession rps = (RaptorPaxosSession) session.customAttachment;
		netHandler.onRecieve(rps, (Buf) receiveObject);
	}

	@Override
	public void onOneThreadMessageSent(ProtocolHandlerIoSession session, int sendSize) {
		// nothing to do
	}

}
