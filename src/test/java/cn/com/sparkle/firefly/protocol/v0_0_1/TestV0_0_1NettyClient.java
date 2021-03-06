package cn.com.sparkle.firefly.protocol.v0_0_1;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.NetHandler;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.firefly.net.netlayer.netty.NettyClient;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.protocolprocessor.filter.FrameUnpackFilter;

public class TestV0_0_1NettyClient implements NetHandler {
	public final static Logger logger = Logger.getLogger(TestV0_0_1NettyClient.class);
	private int cc = 0;
	private long ct = System.currentTimeMillis();
	private ReentrantLock lock = new ReentrantLock();
	private long start = System.currentTimeMillis();
	private long tc = 0;
	private LinkedList<CountDownLatch> l = new LinkedList<CountDownLatch>();
	private ReentrantLock llock = new ReentrantLock();
	final FrameUnpackFilter frameUnpackFilter = new FrameUnpackFilter();

	public TestV0_0_1NettyClient() {
		frameUnpackFilter.setNext(new AbstractChainProtocolProcessor<FrameBody>() {
			@Override
			public void receive(FrameBody t, PaxosSession session) throws InterruptedException {
				t.isValid();
				try{
					llock.lock();
					l.removeFirst().countDown();
				}finally{
					llock.unlock();
				}
				try{
					lock.lock();
					++cc;
					++tc;
					if(cc%10000 == 0){
						long tt = System.currentTimeMillis() - ct;
						System.out.println((cc*1000/tt) + "/s   " + (tc*1000 /(System.currentTimeMillis() - start)) + "/s");
						ct = System.currentTimeMillis();
						cc = 1;
					}
				}finally{
					lock.unlock();
				}
			}

		});
	}

	public static void main(String[] args) throws Throwable {
		logger.debug("start");
		NettyClient client = new NettyClient();
		client.init("target/test-classes/service_in_net1.prop", 20000, new TestV0_0_1NettyClient(),"client");
		client.connect("127.0.0.1", 1234, null);

	}

	@Override
	public void onDisconnect(PaxosSession session) {

	}

	@Override
	public void onConnect(final PaxosSession session, Object connectAttachment) {
		System.out.println("connect");
		for (int i = 0; i < 1; i++) {
			Thread t = new Thread() {
				private byte[][] buff = new byte[][]{new byte[8 * 512],new byte[512]};

				public void run() {
					int i = 0;
					long now = System.currentTimeMillis();
					while (true) {
						try {
							FrameBody body = new FrameBody(buff, ChecksumUtil.INBUILD_CRC32);
							CountDownLatch c = send(body, session);
//							c.await();
						} catch (Exception e) {
							e.printStackTrace();
							break;
						}
						
					}
				}
			};
			t.start();
		}

	}

	@Override
	public void onRecieve(PaxosSession session, Buf buffer) throws InterruptedException {

		frameUnpackFilter.receive(buffer, session);

	}

	@Override
	public void onRefuse(Object connectAttachment) {

	}

	public CountDownLatch send(FrameBody o, PaxosSession session) throws NetCloseException {
		CountDownLatch c;
		try {
			llock.lock();
			c = new CountDownLatch(1);
			l.addLast(c);
		} finally {
			llock.unlock();
		}
		session.write(o);
		return c;
	}
}
