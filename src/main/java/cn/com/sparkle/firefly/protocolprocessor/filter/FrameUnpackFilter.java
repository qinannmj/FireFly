package cn.com.sparkle.firefly.protocolprocessor.filter;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.frame.FrameHead;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession.ObjectProtocolCacheBean;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.util.BytesArrayMaker;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.io.IoBufferArrayInputStream;

public class FrameUnpackFilter extends AbstractChainProtocolProcessor<Buf> {
	private final static Logger logger = Logger.getLogger(FrameUnpackFilter.class);
	private final static byte[] nullBytes = new byte[0];

	@Override
	public void receive(Buf buff, PaxosSession session) throws InterruptedException {
		ObjectProtocolCacheBean bean = session.getProtocolAttachment();
		if (bean.buff.size() == 0 || bean.buff.getLast() != buff) {
			bean.buff.addLast(buff);
			bean.recieveSize += buff.getByteBuffer().remaining();
		}
		FrameBody body = null;
		while ((body = decode(bean, session)) != null) {
			super.fireOnReceive(body, session);
		}
	}

	private FrameBody decode(ObjectProtocolCacheBean bean, PaxosSession session) {

		IoBufferArrayInputStream inputStream = null;
		try {
			if (bean.head == null) {
				if (bean.recieveSize >= 1) {
					//unpack head
					int pos = bean.buff.get(0).getByteBuffer().position();
					byte first = bean.buff.get(0).getByteBuffer().get(pos);
					int checksumLength = FrameHead.calcChecksumLength(first);
					if (bean.recieveSize >= checksumLength + 4) {
						byte[] head = new byte[4];
						byte[] checksum;
						inputStream = new IoBufferArrayInputStream(bean.buff.toArray(new IoBuffer[bean.buff.size()]));
						inputStream.read(head);
						if (checksumLength == 0) {
							checksum = nullBytes;
						} else {
							checksum = new byte[checksumLength];
							inputStream.read(checksum);
						}
						bean.head = new FrameHead(head, checksum);
						// remove and close IoBuffer that has been unuseful.
						while (bean.buff.size() > 0 && !bean.buff.getFirst().getByteBuffer().hasRemaining()) {
							bean.buff.removeFirst().close();
						}
						bean.recieveSize -= checksumLength + 4;
						if (!bean.head.isValid()) {
							logger.warn("tcp checksum invalid, close connection!");
							session.closeSession();
							return null;
						}
					}
				}
			}
			

			if (bean.head != null) {
				//unpack body
				if (bean.recieveSize >= bean.head.getBodySerializeSize()) {
					if (inputStream == null) {
						inputStream = new IoBufferArrayInputStream(bean.buff.toArray(new IoBuffer[bean.buff.size()]));
					}
					byte[][] body = BytesArrayMaker.makeBytesArray(bean.head.getBodySize());
					inputStream.read(body);
					byte[] checksum;
					if (bean.head.getChecksumType() == ChecksumUtil.NO_CHECKSUM) {
						checksum = nullBytes;
					} else {
						checksum = new byte[bean.head.getBodyChecksumLength()];
						try {
							inputStream.read(checksum);
						} catch (RuntimeException e) {
							throw e;
						}
					}
					// remove and close IoBuffer that has been unuseful.
					while (bean.buff.size() > 0 && !bean.buff.getFirst().getByteBuffer().hasRemaining()) {
						bean.buff.removeFirst().close();
					}

					bean.recieveSize -= bean.head.getBodySerializeSize();
					FrameBody frameBody = new FrameBody(body, checksum, bean.head.getChecksumType());
					frameBody.setHead(bean.head);
					bean.head = null;//clear last time head 
					return frameBody;
				}

			}
			return null;
		} catch (Throwable e) {
			logger.error("fatal error", e);
			return null;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
