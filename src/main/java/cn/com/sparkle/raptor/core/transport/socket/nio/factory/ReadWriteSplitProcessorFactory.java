package cn.com.sparkle.raptor.core.transport.socket.nio.factory;

import java.io.IOException;
import java.nio.channels.Selector;

import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketReadProcessor;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketWriteProcessor;

public class ReadWriteSplitProcessorFactory implements ProcessorFactory{

	@Override
	public ProcessorGroup make(NioSocketConfigure nscfg, BuffPool pool, String name) throws IOException {
		NioSocketReadProcessor read = new NioSocketReadProcessor(Selector.open(), nscfg, pool);
		read.startProcessor("readprocessor" + name);
		NioSocketWriteProcessor write = new NioSocketWriteProcessor(Selector.open(), nscfg);
		write.startProcessor("writeprocessor" + name);
		return new ProcessorGroup(read, write);
	}
}
