package cn.com.sparkle.firefly.protocolprocessor.v0_0_1;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.util.BytesArrayMaker;
import cn.com.sparkle.firefly.util.ProtobufUtil;
import cn.com.sparkle.raptor.core.io.BytesArraysOutputStream;

public abstract class AbstractProtocolV0_0_1Processor extends AbstractChainProtocolProcessor<MessagePackage> {
	private final static Logger logger = Logger.getLogger(AbstractProtocolV0_0_1Processor.class);

	public void sendResponse(PaxosSession session, MessagePackage builder) {

		try {
			byte[][] out = ProtobufUtil.transformTo(builder);
			FrameBody body = new FrameBody(out, session.getChecksumType());
			session.write(body);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("unexcepted error", e);
			session.closeSession();
		} catch (NetCloseException e) {
		} catch (IOException e) {
			logger.error("unexcepted error", e);
			session.closeSession();
		}
	}
}
