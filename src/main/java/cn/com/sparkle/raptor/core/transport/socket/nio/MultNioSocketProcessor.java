package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.io.IOException;

import cn.com.sparkle.raptor.core.buff.GroupSyncBuffPool;
import cn.com.sparkle.raptor.core.transport.socket.nio.factory.ProcessorGroup;

public class MultNioSocketProcessor {
	ProcessorGroup[] nioSocketProcessors;
	int currentFlag = 0;

	public MultNioSocketProcessor(NioSocketConfigure nscfg, String name) throws IOException {
		
		nioSocketProcessors = new ProcessorGroup[nscfg.getProcessorNum()];
		int singlePoolSize = nscfg.getCycleRecieveBuffCellSize() / nscfg.getProcessorNum();
		
		for (int i = 0; i < nscfg.getProcessorNum(); i++) {
			GroupSyncBuffPool mempool = new GroupSyncBuffPool(singlePoolSize, nscfg.getCycleRecieveBuffSize(),"recievePool");
//			nioSocketProcessors[i] = new NioSocketProcessor(nscfg, mempool, name);
			nioSocketProcessors[i] = nscfg.getProcessorFactory().make(nscfg, mempool, name);
		}
	}

	public ProcessorGroup getProcessor() {
		currentFlag = (currentFlag + 1) % nioSocketProcessors.length;
		return nioSocketProcessors[currentFlag];
	}
	

}
