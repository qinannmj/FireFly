package cn.com.sparkle.raptor.core.transport.socket.nio.factory;

import java.io.IOException;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;

public interface ProcessorFactory {
	public ProcessorGroup make(NioSocketConfigure nscfg,BuffPool pool,String name) throws IOException;
}
