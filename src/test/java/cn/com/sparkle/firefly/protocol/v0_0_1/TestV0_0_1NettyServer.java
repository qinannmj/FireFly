package cn.com.sparkle.firefly.protocol.v0_0_1;

import java.io.IOException;

import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.net.netlayer.netty.NettyServer;
import cn.com.sparkle.firefly.net.netlayer.raptor.RaptorServer;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.protocolprocessor.filter.FrameUnpackFilter;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;

public class TestV0_0_1NettyServer {

	/**
	 * @param args
	 * @throws Throwable 
	 */
	public static void main(String[] args) throws Throwable {
		final FrameUnpackFilter frameUnpackFilter = new FrameUnpackFilter();
		frameUnpackFilter.setNext(new AbstractChainProtocolProcessor<FrameBody>() {
			@Override
			public void receive(FrameBody t, PaxosSession session) throws InterruptedException {
				t.isValid();
				try {
					
					session.write(t);
				} catch (NetCloseException e) {
					e.printStackTrace();
				}
			}

		});
		NettyServer server = new NettyServer();
		server.init("target/classes/service_out_net.prop", 20000, new NetHandler() {

			@Override
			public void onRefuse(Object connectAttachment) {
			}

			@Override
			public void onRecieve(PaxosSession session, Buf buffer) throws InterruptedException {
				frameUnpackFilter.receive(buffer, session);
			}

			@Override
			public void onDisconnect(PaxosSession session) {
			}

			@Override
			public void onConnect(PaxosSession session, Object connectAttachment) {
			}
		},"server");
		server.listen("127.0.0.1", 1234);
	}

}
