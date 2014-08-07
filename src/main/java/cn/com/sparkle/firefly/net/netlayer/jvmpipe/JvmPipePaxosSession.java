package cn.com.sparkle.firefly.net.netlayer.jvmpipe;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.net.netlayer.buf.ReferenceBuf;

public class JvmPipePaxosSession extends PaxosSession {

	private final static Logger logger = Logger.getLogger(JvmPipePaxosSession.class);
	private volatile boolean isClose = false;

	private NetHandler handler;
	private JvmPipePaxosSession peer;
	private ArrayBlockingQueue<Buf> queue = new ArrayBlockingQueue<Buf>(1000);
	private Thread recieveThread;

	public JvmPipePaxosSession(final NetHandler handler, String flag) {
		this.handler = handler;
		recieveThread = new Thread() {
			public void run() {
				while (!isClose) {
					try {
						Buf msg = queue.take();
						JvmPipePaxosSession.this.handler.onRecieve(JvmPipePaxosSession.this, msg);
					} catch (InterruptedException e) {
						// close elegantly
						closeSession();
					} catch (Throwable e) {
						logger.error("error", e);
						closeSession();
					}
				}
				handler.onDisconnect(JvmPipePaxosSession.this);
				
			}
		};
		recieveThread.setName("pipe-injvm" + flag);
		recieveThread.start();
	}

	public void setPeer(JvmPipePaxosSession peer) {
		this.peer = peer;
	}

	@Override
	public void closeSession() {
		if(!isClose){
			isClose = true;
			recieveThread.interrupt();
			peer.closeSession();
		}
		
	}

	@Override
	public int getChecksumType() {
		return ChecksumUtil.NO_CHECKSUM;
	}

	@Override
	public void write(Buf[] buf) throws NetCloseException {
		synchronized (this) {
			if (isClose) {
				throw new NetCloseException(null);
			}
			for (Buf b : buf) {
				if (b.getByteBuffer().hasRemaining()) {
					try {
						peer.queue.put(b);
					} catch (InterruptedException e) {
						logger.error("unexcepted error", e);
						closeSession();
						throw new NetCloseException(e);
					}
				}
			}
		}
	}

	@Override
	public Buf wrap(byte[] bytes) {

		return new ReferenceBuf(bytes);
	}

	@Override
	public boolean isClose() {
		return isClose;
	}

	@Override
	public String getRemoteAddress() {
		return "jvmpipe";
	}

	@Override
	public String getLocalAddress() {
		return "jvmpipe";
	}
}
