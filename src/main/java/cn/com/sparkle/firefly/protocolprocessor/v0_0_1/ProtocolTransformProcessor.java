package cn.com.sparkle.firefly.protocolprocessor.v0_0_1;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.util.ProtobufUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtocolTransformProcessor extends AbstractChainProtocolProcessor<FrameBody> {
	private final static Logger logger = Logger.getLogger(ProtocolTransformProcessor.class);

	@Override
	public void receive(FrameBody t, PaxosSession session) throws InterruptedException {
		MessagePackage.Builder messagePackage = MessagePackage.newBuilder();
		try {
			messagePackage.mergeFrom(ByteString.copyFrom(ProtobufUtil.transformTo(t.getBody())));
			super.fireOnReceive(messagePackage.build(), session);
		} catch (InvalidProtocolBufferException e) {
			logger.error("fatal error", e);
			session.closeSession();
		}
	}
}
