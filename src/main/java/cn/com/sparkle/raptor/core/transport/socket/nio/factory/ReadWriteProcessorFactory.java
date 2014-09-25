package cn.com.sparkle.raptor.core.transport.socket.nio.factory;

import java.io.IOException;
import java.nio.channels.Selector;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketReadWriteProcessor;

public class ReadWriteProcessorFactory implements ProcessorFactory{

	@Override
	public ProcessorGroup make(NioSocketConfigure nscfg, BuffPool pool, String name) throws IOException {
		NioSocketReadWriteProcessor p = new NioSocketReadWriteProcessor(Selector.open(), nscfg, pool);
		p.startProcessor("rwprocessor" + name);
		return new ProcessorGroup(p, p);
	}

}
