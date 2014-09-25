package cn.com.sparkle.raptor.core.protocol.textline;

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.IoBufferArray;
import cn.com.sparkle.raptor.core.protocol.CodecHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.Protocol;

public class TextLineProtocol implements Protocol {

	@Override
	public Object decode(ProtocolHandlerIoSession session, IoBuffer buff) {

		if (!buff.getByteBuffer().hasRemaining()) {
			buff.close();
			return null;// check buff
		}

		DecodeCache decodeCache = (DecodeCache) session.protocolAttachment;

		char c;
		// deal cached byte
		if (decodeCache.b != null) {
			c = (char) (decodeCache.b << 8 | buff.getByteBuffer().get());
			String r = readChar(c, buff.getByteBuffer(), decodeCache);
			if (r != null)
				return r;
		}

		// deal remaining byte in IoBuff
		int remaining = buff.getByteBuffer().remaining();
		int off = remaining % 2;
		for (int i = 0; i < remaining - off; i = i + 2) {
			c = buff.getByteBuffer().getChar();
			String r = readChar(c, buff.getByteBuffer(), decodeCache);
			if (r != null)
				return r;
		}
		// keep the last odd byte
		if (off == 1) {
			decodeCache.b = buff.getByteBuffer().get();
		} else {
			decodeCache.b = null;
		}
		return null;
	}

	private String readChar(char c, ByteBuffer b, DecodeCache decodeCache) {
		if (c == '\r' || c == '\n') {
			if (decodeCache.sb.length() != 0) {
				String result = decodeCache.sb.toString();
				decodeCache.sb = new StringBuilder();
				return result;
			}
		} else {
			decodeCache.sb.append(c);
		}
		return null;
	}

	public static class DecodeCache {
		private StringBuilder sb = new StringBuilder();
		private Byte b;
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message) throws IOException {
		return encode(buffpool, message, null);
	}

	@Override
	public void init(ProtocolHandlerIoSession session) {
		session.protocolAttachment = new DecodeCache();
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message, IoBuffer lastWaitSendBuff) throws IOException {
		String s = (String) message;
		IoBufferArray newBuffArray;

		IoBufferArray ioBuffArray;
		if (lastWaitSendBuff != null) {
			newBuffArray = buffpool.get(s.length() * 2 + 2 - lastWaitSendBuff.getByteBuffer().remaining());

		} else {
			newBuffArray = buffpool.get(s.length() * 2 + 2);
		}

		if (lastWaitSendBuff != null) {
			IoBuffer[] ioBuffer = new IoBuffer[newBuffArray.getIoBuffArray().length + 1];
			ioBuffer[0] = lastWaitSendBuff;
			System.arraycopy(newBuffArray.getIoBuffArray(), 0, ioBuffer, 1, newBuffArray.getIoBuffArray().length);
			ioBuffArray = new IoBufferArray(ioBuffer);
		} else {
			ioBuffArray = newBuffArray;
		}
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			ioBuffArray.put((byte) (c >> 8));
			ioBuffArray.put((byte) (c & 0xff));
		}
		ioBuffArray.put((byte) 0);
		ioBuffArray.put((byte) '\r');
		return newBuffArray.getIoBuffArray();
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession) throws IOException {
		return null;
	}
}
