package cn.com.sparkle.raptor.core.transport.socket.nio.factory;

import cn.com.sparkle.raptor.core.transport.socket.nio.NioReadProcessor;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioWriteProcessor;

public class ProcessorGroup {
	private NioReadProcessor readProcessor;
	private NioWriteProcessor writeProcessor;
	public NioReadProcessor getReadProcessor() {
		return readProcessor;
	}
	public NioWriteProcessor getWriteProcessor() {
		return writeProcessor;
	}
	public ProcessorGroup(NioReadProcessor readProcessor, NioWriteProcessor writeProcessor) {
		super();
		this.readProcessor = readProcessor;
		this.writeProcessor = writeProcessor;
	}
}
