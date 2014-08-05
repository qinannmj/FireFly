package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;

import cn.com.sparkle.raptor.core.buff.SyncBuffPool;

public class MultNioSocketProcessor {
	NioSocketProcessor[] nioSocketProcessors;
	int currentFlag = 0;

	public MultNioSocketProcessor(NioSocketConfigure nscfg, String name) throws IOException {
		nioSocketProcessors = new NioSocketProcessor[nscfg.getProcessorNum()];
		SyncBuffPool mempool = new SyncBuffPool(nscfg.getCycleRecieveBuffCellSize(), nscfg.getCycleRecieveBuffSize());
		for (int i = 0; i < nscfg.getProcessorNum(); i++) {
			nioSocketProcessors[i] = new NioSocketProcessor(nscfg, mempool, name);
		}
	}

	public NioSocketProcessor getProcessor() {
		currentFlag = (currentFlag + 1) % nioSocketProcessors.length;
		return nioSocketProcessors[currentFlag];
	}

}
