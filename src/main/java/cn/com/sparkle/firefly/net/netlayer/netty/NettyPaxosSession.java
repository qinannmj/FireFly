package cn.com.sparkle.firefly.net.netlayer.netty;

import io.netty.channel.ChannelHandlerContext;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;

public class NettyPaxosSession extends PaxosSession {

	private ChannelHandlerContext ctx;
	private volatile boolean isClose = false;

	public NettyPaxosSession(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void closeSession() {
		ctx.close();
	}

	@Override
	public void write(Buf[] buf) throws NetCloseException {
		ctx.write(buf);
		ctx.flush();
	}

	@Override
	public Buf wrap(byte[] bytes) {
		return ReferenceNettyByteBuf.refer(bytes);
	}

	@Override
	public boolean isClose() {
		return isClose;
	}
	
	public void setIsClose(boolean isClose){
		this.isClose = isClose;
	}

}
