package cn.com.sparkle.firefly.net.netlayer;

import java.io.IOException;

import cn.com.sparkle.firefly.net.netlayer.jvmpipe.JvmPipeClient;
import cn.com.sparkle.firefly.net.netlayer.jvmpipe.JvmPipeServer;
import cn.com.sparkle.firefly.net.netlayer.netty.NettyClient;
import cn.com.sparkle.firefly.net.netlayer.netty.NettyServer;
import cn.com.sparkle.firefly.net.netlayer.raptor.RaptorClient;
import cn.com.sparkle.firefly.net.netlayer.raptor.RaptorServer;

public class NetFactory {

	public static NetClient makeClient(String type) throws IOException {
		if (type.equals("raptor")) {
			return new JvmPipeClient(new RaptorClient());
		} else {
			return new JvmPipeClient(new NettyClient());
		}
	}

	public static NetServer makeServer(String type) throws IOException {
		if (type == null) {
			throw new RuntimeException("unspported net layer :" + type);
		}
		if ("raptor".equals(type.toLowerCase())) {
			return new JvmPipeServer(new RaptorServer());
		} else if ("netty".equals(type.toLowerCase())) {
			return new JvmPipeServer(new NettyServer());
		} else {
			throw new RuntimeException("unspported net layer :" + type);
		}
	}
}
