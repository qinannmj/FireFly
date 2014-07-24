package cn.com.sparkle.paxos.net.netlayer.buf;

import cn.com.sparkle.raptor.core.buff.IoBuffer;

public interface Buf extends IoBuffer {
	public Buf duplicateBuf();

	public void close();
}
