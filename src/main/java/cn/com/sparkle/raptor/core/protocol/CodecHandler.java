package cn.com.sparkle.raptor.core.protocol;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.GroupSyncBuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.ProxyIoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class CodecHandler implements IoHandler {
	private final static Logger logger = Logger.getLogger(CodecHandler.class);

	private GroupSyncBuffPool buffPool;
	private Protocol protocol;
	private IoHandler handler;
	private Protocol nullProtocol = new Protocol() {
		@Override
		public Object decode(ProtocolHandlerIoSession attachment, IoBuffer buff) {
			return buff;
		}

		@Override
		public IoBuffer[] encode(BuffPool Buffpool, Object obj) {
			throw new RuntimeException("not supported method");
		}

		@Override
		public void init(ProtocolHandlerIoSession attachment) {
		}

		@Override
		public Object decode(ProtocolHandlerIoSession mySession) throws IOException {
			return null;
		}

		@Override
		public IoBuffer[] encode(BuffPool buffpool, Object message, IoBuffer lastWaitSendBuff) throws IOException {
			return null;
		}
	};

	public CodecHandler(int sendBuffTotalCellSize, int sendBuffCellCapacity, Protocol protocol, IoHandler handler) {
		buffPool = new GroupSyncBuffPool(sendBuffTotalCellSize, sendBuffCellCapacity,"sendPool");
		this.protocol = protocol;
		this.handler = handler;
		if (protocol == null) {
			protocol = nullProtocol;
		}
	}

	@Override
	public final void onSessionOpened(IoSession session) {

		ProtocolHandlerIoSession mySession = new ProtocolHandlerIoSession(session, protocol == nullProtocol ? null : protocol, buffPool);
		mySession.customAttachment = session.attachment();
		session.attach(mySession);
		protocol.init(mySession);
		this.handler.onSessionOpened(mySession);
	}

	@Override
	public void onSessionClose(IoSession session) {
		handler.onSessionClose((ProtocolHandlerIoSession) session.attachment());
	}

	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
		final ProtocolHandlerIoSession attachment = (ProtocolHandlerIoSession) session.attachment();
		attachment.recivePackageCount = (attachment.recivePackageCount + 1) % Integer.MAX_VALUE;
		final int recivePackageCount = attachment.recivePackageCount;
		try {
			Object obj = protocol.decode(attachment, (IoBuffer) message);
			attachment.targetRecivePackageCount = recivePackageCount;
			while (obj != null) {
				try {
					handler.onMessageRecieved(attachment, obj);
					obj = protocol.decode(attachment);
				} catch (Throwable e) {
					logger.error("error", e);
					session.closeSession();
					break;
				}
			}

		} catch (Exception e) {
			logger.error("fatal error", e);
			session.closeSession();
		}
	}

	@Override
	public void onMessageSent(IoSession session, int size) {
		handler.onMessageSent((ProtocolHandlerIoSession) session.attachment(), size);
	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		handler.catchException(session.attachment() instanceof ProtocolHandlerIoSession ? (ProtocolHandlerIoSession) session.attachment() : session, e);
	}

	public static class ProtocolHandlerIoSession extends ProxyIoSession {
		public Object protocolAttachment;
		public Object customAttachment;
		private volatile int recivePackageCount = 0;
		private int targetRecivePackageCount = 0;

		private ReentrantLock writeLock = new ReentrantLock();

		private Protocol protocol;
		private BuffPool buffPool;

		public ProtocolHandlerIoSession(final IoSession session, Protocol protocol, BuffPool buffPool) {
			super(session);
			this.protocol = protocol;
			this.buffPool = buffPool;
		}

		@Override
		public boolean tryWrite(IoBuffer message, boolean flush) throws SessionHavaClosedException {
			try {
				writeLock.lock();
				return super.tryWrite(message, flush);
			} catch (SessionHavaClosedException e) {
				message.close();
				throw e;
			} finally {
				writeLock.unlock();
			}
		}

		public void write(Object msg, boolean tryFlushRightNow) throws SessionHavaClosedException {
			ReentrantLock lock = null;
			try {
				lock = writeLock;
				lock.lock();
				IoBuffer lastBuff = this.getLastWaitSendBuffer();
				if (protocol == null) {
					throw new RuntimeException("not supported method");
				}
				IoBuffer[] buffs = protocol.encode(buffPool, msg,lastBuff);
				if(lastBuff != null){
					flushLastWaitSendBuffer(lastBuff);
				}
				for (int i = 0; i < buffs.length; ++i) {
					try {
						super.write(buffs[i], targetRecivePackageCount == recivePackageCount);
					} catch (SessionHavaClosedException e) {
						for (; i < buffs.length; ++i) {
							buffs[i].close();
						}
						throw e;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (lock != null) {
					lock.unlock();
				}
			}
		}
		/*public void write(Object msg, boolean tryFlushRightNow) throws SessionHavaClosedException {
			ReentrantLock lock = null;
			try {
				//				lock = writeLock;
				//				lock.lock();
				//				IoBuffer lastBuff = this.getLastWaitSendBuffer();
				IoBuffer lastBuff = null;
				if (protocol == null) {
					throw new RuntimeException("not supported method");
				}
				IoBuffer[] buffs = protocol.encode(buffPool, msg, lastBuff);
				//				if(lastBuff != null){
				//					flushLastWaitSendBuffer(lastBuff);
				//				}

				try {
					if (buffs.length > 1) {
						lock = writeLock;
						lock.lock();
					}
					for (int i = 0; i < buffs.length; ++i) {
						try {
							super.write(buffs[i], targetRecivePackageCount == recivePackageCount);
						} catch (SessionHavaClosedException e) {
							for (; i < buffs.length; ++i) {
								buffs[i].close();
							}
							throw e;
						}
					}
				} finally {
					if (lock != null) {
						lock.unlock();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			//			finally {
			//				if (lock != null) {
			//					lock.unlock();
			//				}
			//			}
		}*/
		@Override
		public void attach(Object attachment) {
			this.customAttachment = attachment;
		}

		@Override
		public Object attachment() {
			return this.customAttachment;
		}
	}

}
