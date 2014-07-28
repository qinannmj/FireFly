package cn.com.sparkle.firefly.net.netlayer.netty;

import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class BufArrayEncoder extends ChannelOutboundHandlerAdapter {
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {
			Buf[] bufs = (Buf[]) msg;
			for (Buf buf : bufs) {
				ByteBuf b = (ByteBuf) buf;
				if (b.isReadable()) {
					super.write(ctx, buf, promise);
				} else {
					b.release();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
