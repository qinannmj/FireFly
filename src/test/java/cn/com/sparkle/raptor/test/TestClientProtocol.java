package cn.com.sparkle.raptor.test;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.SyncBuffPool;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.Protocol;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.protocol.textline.TextLineProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestClientProtocol {
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		NioSocketClient client = new NioSocketClient(nsc);
		IoHandler handler = new MultiThreadProtecolHandler(1000, 1024, 20, 300, 60, TimeUnit.SECONDS,new TextLineProtocol(), new TestProtocolClientHandler());
		for(int i = 0 ; i < 1 ; i++){
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler);
//			client.connect(new InetSocketAddress("127.0.0.1",1234),handler );
			client.connect(new InetSocketAddress("192.168.3.100",1234),handler );
		}
	}

}
class TestProtocolClientHandler implements ProtocolHandler{
	private static AtomicInteger flag = new AtomicInteger(0);
	private int i = 0;
	@Override
	public void onOneThreadSessionOpen(ProtocolHandlerIoSession session) {
		try {
			
//			ProtecolHandlerAttachment att = (ProtecolHandlerAttachment)session.attachment();
			session.customAttachment = Integer.valueOf(flag.addAndGet(1));
			System.out.println(session.customAttachment);
			session.writeObject("Hello,Mr server!");
		} catch (SessionHavaClosedException e) {
			//only stop send data,because the onOneThreadSessionClose will be invoked subsequently. 
			return;
		}
	}

	@Override
	public void onOneThreadSessionClose(ProtocolHandlerIoSession session) {
		System.out.println("close" + session.customAttachment);
	}

	@Override
	public void onOneThreadCatchException(IoSession ioSession,
			ProtocolHandlerIoSession attachment, Throwable e) {
		e.printStackTrace();
	}
	private AtomicInteger c = new AtomicInteger(0) ;
	private long ct = System.currentTimeMillis();
	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
//		System.out.println(o);
		int cc = c.getAndAdd(1);
		if(cc%10000 == 0){
			long tt = System.currentTimeMillis() - ct;
			System.out.println((cc*1000/tt) + "/s");
		}
		try {
				session.writeObject("ÄãºÃ£¡Mr server!This is client" + session.customAttachment + "!write package" + (++i));
		} catch (SessionHavaClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void onOneThreadMessageSent(ProtocolHandlerIoSession session,int sendSize) {
		// TODO Auto-generated method stub
		
	}

	
	
}