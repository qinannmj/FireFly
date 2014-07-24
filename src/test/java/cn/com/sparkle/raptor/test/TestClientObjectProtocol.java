package cn.com.sparkle.raptor.test;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.SyncBuffPool;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.Protocol;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.protocol.javaobject.ObjectProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestClientObjectProtocol {
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		NioSocketClient client = new NioSocketClient(nsc);
		nsc.setProcessorNum(1);
		nsc.setTcpNoDelay(true);
		nsc.setCycleRecieveBuffCellSize(10000);
		nsc.setSentBuffSize(1024);
		IoHandler handler = new MultiThreadProtecolHandler(1000, 512, 20, 300, 60, TimeUnit.SECONDS,new ObjectProtocol(), new TestProtocolObjetClientHandler());
		for(int i = 0 ; i < 1 ; i++){
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler,"aaa" + i);
			
//			client.connect(new InetSocketAddress("192.168.3.100",1234),handler,"aaa" + i );
			client.connect(new InetSocketAddress("127.0.0.1",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.128.11",1234),handler,"aaa" + i );
		}
	}

}
class TestProtocolObjetClientHandler implements ProtocolHandler{
	private static AtomicInteger flag = new AtomicInteger(0);
	private int i = 0;
	private String test = "ÄãºÃ£¡Mr server !This is client  cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";
	public TestProtocolObjetClientHandler() {
		for(int i = 0 ; i < 1 ; i++){
			test += test;
		}
//		test ="ccc";
	}
	@Override
	public void onOneThreadSessionOpen(ProtocolHandlerIoSession session) {
		try {
			System.out.println("init attachment:" + session.customAttachment);
			session.customAttachment = Integer.valueOf(flag.addAndGet(1));
			session.writeObject("Hello,Mr server!");
		} catch (SessionHavaClosedException e) {
//			//only stop send data,because the onOneThreadSessionClose will be invoked subsequently. 
			return;
		}
	}


	private int cc = 0 ;
	private long ct = System.currentTimeMillis();
	private ReentrantLock lock = new ReentrantLock();
	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
//		System.out.println(o);
		try {
			session.writeObject(test);
	} catch (SessionHavaClosedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		try{
			lock.lock();
			++cc;
			if(cc%10000 == 0){
				long tt = System.currentTimeMillis() - ct;
				System.out.println((cc*1000/tt) + "/s");
				ct = System.currentTimeMillis();
				cc = 1;
			}
		}finally{
			lock.unlock();
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


	@Override
	public void onOneThreadMessageSent(ProtocolHandlerIoSession session,int sendSize) {
		
	}
	
	
}