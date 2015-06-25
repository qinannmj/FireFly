package cn.com.sparkle.firefly.net.netlayer.netty;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {

	private NetHandler handler;
	public final static AttributeKey<Object> attachKey = new AttributeKey<Object>("attach");

	public NettyHandler(NetHandler handler) {
		this.handler = handler;
	}

	private final static Logger logger = Logger.getLogger(NettyHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		NettyPaxosSession session = new NettyPaxosSession(ctx);
		Attribute<Object> attr = ctx.channel().attr(attachKey);
		Object attachment = attr.get();
		attr.set(session);
		handler.onConnect(session, attachment);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Attribute<Object> attr = ctx.channel().attr(attachKey);
		NettyPaxosSession session = (NettyPaxosSession) attr.get();
		session.setIsClose(true);
		handler.onDisconnect(session);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Attribute<Object> attr = ctx.channel().attr(attachKey);
		NettyPaxosSession session = (NettyPaxosSession) attr.get();
		Buf buf = (Buf) msg;
		handler.onRecieve(session, buf);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		System.out.println("fffffffffffffffff");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println(ctx.channel().remoteAddress().toString());
		logger.error("unexcepted exception", cause);
		ctx.close();
	}
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent event = (IdleStateEvent)evt;
			if(event.state() == IdleState.READER_IDLE){
				ctx.close();
			}
		}else{
			super.userEventTriggered(ctx, evt);
		}
	}
}
