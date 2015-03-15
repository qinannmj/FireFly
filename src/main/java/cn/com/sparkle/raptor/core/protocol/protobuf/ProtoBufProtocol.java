package cn.com.sparkle.raptor.core.protocol.protobuf;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.io.BufferPoolOutputStream;
import cn.com.sparkle.raptor.core.io.IoBufferArrayInputStream;
import cn.com.sparkle.raptor.core.protocol.CodecHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.DecodeException;
import cn.com.sparkle.raptor.core.protocol.Protocol;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;

public class ProtoBufProtocol implements Protocol {
	private Logger logger = Logger.getLogger(ProtoBufProtocol.class);
	private GeneratedMessage generatedMessage;

	public ProtoBufProtocol(GeneratedMessage generatedMessage) {
		this.generatedMessage = generatedMessage;
	}

	@Override
	public void init(ProtocolHandlerIoSession session) {
		session.protocolAttachment = new ObjectProtocolCacheBean();
	}

	private static class ObjectProtocolCacheBean {

		private int curPackageSize = 0;
		private int recieveSize = 0;
		private byte checkPos = 0;
		private LinkedList<IoBuffer> buff = new LinkedList<IoBuffer>();
	}

	private void readSize(ObjectProtocolCacheBean bean) {
		if (bean.buff.size() == 0) {
			return;
		}
		IoBuffer buff = bean.buff.getFirst();
		for (int i = 1; i < 6 - bean.checkPos; i++) {
			if (bean.recieveSize > i) {
				byte tmp = buff.getByteBuffer().get();
				--bean.recieveSize;
				bean.curPackageSize |= (tmp & 0x7f) << (7 * bean.checkPos);
				if (!buff.getByteBuffer().hasRemaining()) {
					bean.buff.removeFirst().close();
					if (bean.buff.size() != 0) {
						buff = bean.buff.getFirst();
					}
				}
				if (tmp >= 0) {
					bean.checkPos = -1;
					return;
				} else {
					++bean.checkPos;
				}
			}
		}
	}

	@Override
	public Object decode(ProtocolHandlerIoSession session, IoBuffer buff) throws DecodeException {
		ObjectProtocolCacheBean bean = (ObjectProtocolCacheBean) session.protocolAttachment;
		if (bean.buff.size() == 0 || bean.buff.getLast() != buff) {
			bean.buff.addLast(buff);
			bean.recieveSize += buff.getByteBuffer().remaining();
		}
		return decode(session);

	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message) throws IOException {
		return encode(buffpool, message, null);
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message, IoBuffer lastWaitSendBuff) throws IOException {
		GeneratedMessage generatedMessage = (GeneratedMessage) message;
		int dataSize = generatedMessage.getSerializedSize();
		BufferPoolOutputStream bufferPoolOutputStream = new BufferPoolOutputStream(buffpool, 0, lastWaitSendBuff);
		// write count to buff
		while (true) {
			if ((dataSize & ~0x7FL) == 0) {
				bufferPoolOutputStream.write(dataSize);
				break;
			} else {
				bufferPoolOutputStream.write((((int) dataSize & 0x7F) | 0x80));
				dataSize >>>= 7;
			}
		}
		generatedMessage.writeTo(bufferPoolOutputStream);
		List<IoBuffer> list = bufferPoolOutputStream.getBuffArray();
		if (lastWaitSendBuff != null) {
			list.remove(0);
		}
		return list.toArray(new IoBuffer[list.size()]);
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession) throws DecodeException {
		ObjectProtocolCacheBean bean = (ObjectProtocolCacheBean) mySession.protocolAttachment;
		if (bean.checkPos >= 0) {
			readSize(bean);
			if (bean.checkPos >= 0) {
				return null;
			}
		}
		if (bean.recieveSize >= bean.curPackageSize) {
			//			int index = getIndex(bean);
			//			if(map[index] == null){
			//				throw new DecodeException("The message is not registered to protocol! id:" + index);
			//			}
			try {
				Builder b = generatedMessage.newBuilderForType().mergeFrom(
						new IoBufferArrayInputStream(bean.buff.toArray(new IoBuffer[bean.buff.size()]), bean.curPackageSize));
				bean.recieveSize -= bean.curPackageSize;
				bean.curPackageSize = 0;
				bean.checkPos = 0;
				// remove and close IoBuffer that has been unuseful.
				while (bean.buff.size() > 0 && !bean.buff.getFirst().getByteBuffer().hasRemaining()) {
					bean.buff.removeFirst().close();
				}
				return b.build();
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("bean.recieveSize" + bean.recieveSize + " bean.curPackageSize:" + bean.curPackageSize);
				}
				throw new DecodeException(e);
			}
		}
		return null;
	}

}
