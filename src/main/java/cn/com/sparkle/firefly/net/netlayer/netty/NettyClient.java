package cn.com.sparkle.firefly.net.netlayer.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;

public class NettyClient implements NetClient {
	private final static Logger logger = Logger.getLogger(NettyClient.class);
	private Bootstrap bootstrap = new Bootstrap();
	private NetHandler netHandler = null;

	private class ConnectFuture implements Future<Boolean> {
		private boolean value;

		public ConnectFuture(boolean value) {
			this.value = value;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			return value;
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return value;
		}
	};

	@Override
	public Future<Boolean> connect(String ip, int port, Object connectAttachment) throws Throwable {
		Bootstrap b = bootstrap.clone();
		b.attr(NettyHandler.attachKey, connectAttachment);
		ChannelFuture f = b.connect(InetAddress.getByName(ip), port);
		f.await();
		if (f.isSuccess()) {
			return new ConnectFuture(true);
		} else {
			logger.error("connect exception", f.cause());
			netHandler.onRefuse(connectAttachment);
			return new ConnectFuture(false);
		}
	}

	@Override
	public void init(String path, final int heartBeatInterval, final NetHandler netHandler, String threadName) throws Throwable {
		final Conf conf = new Conf(path);
		this.netHandler = netHandler;
		NioEventLoopGroup group = new NioEventLoopGroup(conf.getIothreadnum());
		bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.SO_SNDBUF, conf.getSendBuf()).option(ChannelOption.SO_RCVBUF, conf.getRecvBuf())
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new IdleStateHandler((int) (2 * heartBeatInterval / 1000), 0, 0));
						if (conf.getWorkthreadNum() == 0) {
							ch.pipeline().addLast(new BufDecoder(), new BufArrayEncoder(), new NettyHandler(netHandler));
						} else {
							ch.pipeline().addLast(new DefaultEventExecutorGroup(conf.getWorkthreadNum()), new BufDecoder(), new BufArrayEncoder(),
									new NettyHandler(netHandler));
						}
					}
				});
	}

	@Override
	public NetHandler getHandler() {
		return this.netHandler;
	}

}
