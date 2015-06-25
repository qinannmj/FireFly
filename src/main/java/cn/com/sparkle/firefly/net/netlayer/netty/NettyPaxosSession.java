package cn.com.sparkle.firefly.net.netlayer.netty;

import io.netty.buffer.ByteBuf;
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
	public void write(Buf[] bufs) throws NetCloseException {
		for(int i = 0 ;i < bufs.length - 1;++i){
			ByteBuf b = (ByteBuf) bufs[i];
			ctx.write(b);
		}
		if(bufs.length > 0){
			ctx.writeAndFlush(bufs[bufs.length - 1]);
		}
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

	@Override
	public String getRemoteAddress() {
		return ctx.channel().remoteAddress().toString();
	}

	@Override
	public String getLocalAddress() {
		return ctx.channel().localAddress().toString();
	}

}
