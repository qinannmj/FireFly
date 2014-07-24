package cn.com.sparkle.paxos.protocolprocessor.filter;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.net.frame.FrameBody;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.AbstractChainProtocolProcessor;

public class ConnectCheckFilter extends AbstractChainProtocolProcessor<FrameBody> {

	private final static Logger logger = Logger.getLogger(ConnectCheckFilter.class);

	@Override
	public void receive(FrameBody body, PaxosSession session) throws InterruptedException {
		if (body.isValid()) {
			fireOnReceive(body, session);// go to next process
		} else {
			logger.info("The checksum of Tcp  is invalid, close connection !");
			session.closeSession();
		}
	}

}
