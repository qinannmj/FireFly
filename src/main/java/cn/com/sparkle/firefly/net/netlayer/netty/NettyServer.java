package cn.com.sparkle.firefly.net.netlayer.netty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.NetServer;

public class NettyServer implements NetServer {
	private ServerBootstrap bootstrap = new ServerBootstrap();
	private String ip = null;
	private int port;
	@Override
	public void init(String confPath, final int heartBeatInterval, final NetHandler handler,String ip,int port, String threadName) throws FileNotFoundException, IOException {
		final Conf conf = new Conf(confPath);
		this.ip = ip;
		this.port = port;
		NioEventLoopGroup group = new NioEventLoopGroup(conf.getIothreadnum());
		bootstrap.group(group).channel(NioServerSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.SO_SNDBUF, conf.getSendBuf()).option(ChannelOption.SO_RCVBUF, conf.getRecvBuf())
				.option(ChannelOption.SO_BACKLOG, conf.getBacklog()).childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new IdleStateHandler((int) (2 * heartBeatInterval / 1000), 0, 0));

						if (conf.getWorkthreadNum() == 0) {
							ch.pipeline().addLast(new BufDecoder(), new NettyHandler(handler));
						} else {
							ch.pipeline().addLast(new DefaultEventExecutorGroup(conf.getWorkthreadNum()), new BufDecoder(),
									new NettyHandler(handler));
						}
					}
				});
	}

	@Override
	public void listen() throws Throwable {
		if(ip == null) {
			throw new RuntimeException("Not initialize the server!Please run init method!");
		}
		ChannelFuture f = bootstrap.bind(InetAddress.getByName(ip), port);
		f.await();
		if (!f.isSuccess()) {
			throw f.cause();
		}
	}

}
