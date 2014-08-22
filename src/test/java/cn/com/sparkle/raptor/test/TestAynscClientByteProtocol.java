package cn.com.sparkle.raptor.test;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestAynscClientByteProtocol {
	public final static Logger logger = Logger.getLogger(TestAynscClientByteProtocol.class);
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setTcpNoDelay(true);
		nsc.setProcessorNum(1);
//		nsc.setSoLinger(5);
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setRecieveBuffSize(128 * 1024);
		nsc.setSentBuffSize(128 * 1024);
		NioSocketClient client = new NioSocketClient(nsc);
		System.out.println(50);
		IoHandler handler = new MultiThreadProtecolHandler(1000,  32*1024, 2, 300, 60, TimeUnit.SECONDS,new ByteProtocol(100), new TestAsyncByteObjetClientHandler());
		for(int i = 0 ; i < 1; i++){
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler,"aaa" + i);
//			client.connect(new InetSocketAddress("192.168.2.131",1234),handler,"aaa" + i );
			client.connect(new InetSocketAddress("127.0.0.1",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.101.106.91",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.35.16",1234), handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.128.11",1234),handler,"aaa" + i );
			
		}
		logger.warn("sssssss");
	}

}

class TestAsyncByteObjetClientHandler implements ProtocolHandler{
	
	private static AtomicInteger flag = new AtomicInteger(0);
//	private int i = 0;
	private String soure = "ÄãºÃ£¡Mr server !This is client  cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";
	private String test = "";
	public TestAsyncByteObjetClientHandler(){
		for(int i = 0 ; i < 1 ;i++){
			test += soure;
		}
//		test = "fsfwef";
	}
	
	private LinkedList<CountDownLatch> l = new LinkedList<CountDownLatch>();
	private ReentrantLock llock = new ReentrantLock();
	public CountDownLatch send(Object o,ProtocolHandlerIoSession session) throws SessionHavaClosedException{
		
			CountDownLatch c = new CountDownLatch(1);
			try{
				llock.lock();
				l.addLast(c);
			}finally{
				llock.unlock();
			}
			session.writeObject(o);
			return c;
		
		
	}
	@Override
	public void onOneThreadSessionOpen(final ProtocolHandlerIoSession session) {
		for(int i = 0 ; i < 100 ; i++){
		Thread t = new Thread(){
			public void run(){
				int i = 0;
				while(true){
//				for(int j = 0 ; j < 10000 ; j ++){
					try {
//						Thread.sleep(1000);
						long ct = System.currentTimeMillis();
						CountDownLatch c = send(test, session);
						if(System.currentTimeMillis() - ct > 2000){
							TestAynscClientByteProtocol.logger.debug("send more than 2000ms");
//							System.exit(1);
						}
						
//						Thread.sleep(1000);
						c.await();
					} catch (SessionHavaClosedException e) {
						e.printStackTrace();
						break;
					} catch(Exception e){
						e.printStackTrace();
					}
//					catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					
				}
			}
		};
		t.start();
		}
//		IoBuffer[] buffa = protocol.encode(buffPool, "ÄãºÃ£¡Mr server ccccccccccccccc!This is client" + attachment.customAttachment + "!write package" + (++i));
//		System.out.println(buffa[0].getByteBuffer().capacity() - buffa[0].getByteBuffer().remaining());
//		try {
//			session.write(buffa);
//		} catch (SessionHavaClosedException e) {
//		}
	}

	@Override
	public void onOneThreadSessionClose(ProtocolHandlerIoSession session) {
		System.out.println("close" + session.customAttachment);
	}

	private int cc = 0 ;
	private long ct = System.currentTimeMillis();
	private ReentrantLock lock = new ReentrantLock();
	private long start = System.currentTimeMillis();
	private long tc = 0;
	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
//		System.out.println(o);
//		try {
//			IoBuffer[] buffa = protocol.encode(buffPool, "ÄãºÃ£¡Mr server ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc                                                                                                                                                                                                                                                    !This is client" + attachment.customAttachment + "!write package" + (++i));
//			session.write(buffa);
//	} catch (SessionHavaClosedException e) {
//		e.printStackTrace();
//	}
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
			if(cc%20000 == 0){
				long tt = System.currentTimeMillis() - ct;
				try{
					System.out.println((cc*1000/tt) + "/s   " + (tc * 1000/(System.currentTimeMillis() - start) ) + "/s");
					ct = System.currentTimeMillis();
					cc = 1;
				}catch(Throwable e){
					System.out.println(tt);
				}
			}
		}finally{
			lock.unlock();
		}
		
	}
	@Override
	public void onOneThreadCatchException(IoSession ioSession,
			ProtocolHandlerIoSession attachment, Throwable e) {
		e.printStackTrace();
	}
	@Override
	public void onOneThreadMessageSent(ProtocolHandlerIoSession session,int sendSize) {
//		System.out.println(sendSize);
	}

	
}