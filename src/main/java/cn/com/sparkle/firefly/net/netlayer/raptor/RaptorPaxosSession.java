package cn.com.sparkle.firefly.net.netlayer.raptor;

import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.net.netlayer.buf.ReferenceBuf;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class RaptorPaxosSession extends PaxosSession {
	ProtocolHandlerIoSession session;

	public RaptorPaxosSession(ProtocolHandlerIoSession session) {
		this.session = session;
	}

	@Override
	public void closeSession() {
		session.closeSession();
	}

	@Override
	public void write(Buf[] buf) throws NetCloseException {
		try {
			session.writeObject(buf);
		} catch (SessionHavaClosedException e) {
			throw new NetCloseException(e);
		}
	}

	@Override
	public Buf wrap(byte[] bytes) {

		return new ReferenceBuf(bytes);
	}

	@Override
	public boolean isClose() {
		return session.isClose();
	}

}