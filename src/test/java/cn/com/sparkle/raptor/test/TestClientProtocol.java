package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.textline.TextLineProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestClientProtocol {
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		NioSocketClient client = new NioSocketClient(nsc);
		IoHandler handler = new MultiThreadHandler(20, 300,60, TimeUnit.SECONDS, new CodecHandler(1000, 1024, new TextLineProtocol(), new TestProtocolClientHandler()));
		for (int i = 0; i < 1; i++) {
			//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler);
			//			client.connect(new InetSocketAddress("127.0.0.1",1234),handler );
			client.connect(new InetSocketAddress("192.168.3.100", 1234), handler);
		}
	}

}

class TestProtocolClientHandler implements IoHandler {
	private static AtomicInteger flag = new AtomicInteger(0);
	private int i = 0;




	private AtomicInteger c = new AtomicInteger(0);
	private long ct = System.currentTimeMillis();



	@Override
	public void onSessionOpened(IoSession session) {
		try {

			//			ProtecolHandlerAttachment att = (ProtecolHandlerAttachment)session.attachment();
			session.attach(Integer.valueOf(flag.addAndGet(1)));
			System.out.println(session.attachment());
			session.write("Hello,Mr server!", false);
		} catch (SessionHavaClosedException e) {
			//only stop send data,because the onOneThreadSessionClose will be invoked subsequently. 
			return;
		}
		
	}

	@Override
	public void onSessionClose(IoSession session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
//		System.out.println(o);
		int cc = c.getAndAdd(1);
		if (cc % 10000 == 0) {
			long tt = System.currentTimeMillis() - ct;
			System.out.println((cc * 1000 / tt) + "/s");
		}
		try {
			session.write("ÄãºÃ£¡Mr server!This is client" + session.attachment() + "!write package" + (++i),false);
		} catch (SessionHavaClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void onMessageSent(IoSession session, int sendSize) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		e.printStackTrace();
		
	}

}