package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.util.LinkedList;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.IoBufferArray;
import cn.com.sparkle.raptor.core.buff.BuffPool.PoolEmptyException;
import cn.com.sparkle.raptor.core.protocol.Protocol;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;

public class ByteProtocol implements Protocol {
	private int byteSize;
	private byte[] message;

	public ByteProtocol(int byteSize) {
		System.out.println("bytesize:" + byteSize);
		this.byteSize = byteSize;
		message = new byte[byteSize];
	}

	private static class ByteProtocolAttachment {
		int recieveSize = 0;
		LinkedList<IoBuffer> ll = new LinkedList<IoBuffer>();
	}

	@Override
	public void init(ProtocolHandlerIoSession mySession) {
		mySession.protocolAttachment = new ByteProtocolAttachment();
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession, IoBuffer buff) {
		ByteProtocolAttachment att = (ByteProtocolAttachment) mySession.protocolAttachment;

		if (att.ll.size() == 0 || att.ll.getLast() != buff) {
			att.ll.addLast(buff);
			att.recieveSize += buff.getByteBuffer().remaining();
		}
		return decode(mySession);
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message) throws PoolEmptyException {
		return encode(buffpool, message, null);
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message,
			IoBuffer lastWaitSendBuff) throws PoolEmptyException{
		IoBuffer[] buffs;
		IoBufferArray iba;
		if (lastWaitSendBuff != null) {
			iba = buffpool.tryGet(byteSize
					- lastWaitSendBuff.getByteBuffer().remaining());
		} else {
			iba = buffpool.tryGet(byteSize);
		}
		if(iba == null){
			throw new PoolEmptyException();
		}
		if (lastWaitSendBuff != null) {
			buffs = new IoBuffer[1 + iba.getIoBuffArray().length];
			buffs[0] = lastWaitSendBuff;
			System.arraycopy(iba.getIoBuffArray(), 0, buffs, 1,
					buffs.length - 1);
		} else {
			buffs = iba.getIoBuffArray();
		}

		IoBufferArray newArray = new IoBufferArray(buffs);
		newArray.put(this.message);
		return iba.getIoBuffArray();
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession) {
		ByteProtocolAttachment att = (ByteProtocolAttachment) mySession.protocolAttachment;
		if (att.recieveSize >= byteSize) {
			int tbs = byteSize;
			while (tbs > 0) {
				IoBuffer tib = att.ll.getFirst();
				int skip = Math.min(tbs, tib.getByteBuffer().remaining());
				tbs -= skip;
				tib.getByteBuffer().position(
						tib.getByteBuffer().position() + skip);
				if (!tib.getByteBuffer().hasRemaining()) {
					att.ll.removeFirst().close();;
				}
			}
			att.recieveSize -= byteSize;
			return message;
		} else {
			return null;
		}
	}

}
