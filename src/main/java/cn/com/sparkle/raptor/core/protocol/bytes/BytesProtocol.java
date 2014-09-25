package cn.com.sparkle.raptor.core.protocol.bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.com.sparkle.raptor.core.protocol.frame.VariLenFrameProtocol;

public class BytesProtocol extends VariLenFrameProtocol{

	@Override
	public int dataSize(Object msg) {
		byte[] b = (byte[])msg;
		return b.length;
	}

	@Override
	public void flushMsg(OutputStream out, Object msg) throws IOException {
		byte[] b = (byte[])msg;
		out.write(b);
	}

	@Override
	public Object readMsg(InputStream in,int msgSize) throws IOException {
		byte[] b = new byte[msgSize];
		in.read(b);
		return b;
	}

}
