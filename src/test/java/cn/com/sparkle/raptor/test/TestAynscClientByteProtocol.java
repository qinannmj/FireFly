package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.bytes.BytesProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestAynscClientByteProtocol {
	public final static Logger logger = Logger.getLogger(TestAynscClientByteProtocol.class);
	public static void main(String[] args) throws Exception {
//		start();
		start();
	}
	
	public static void start() throws Exception{
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setTcpNoDelay(true);
		nsc.setProcessorNum(24);
//		nsc.setSoLinger(5);
		nsc.setCycleRecieveBuffCellSize(10000);
		nsc.setCycleRecieveBuffSize(4 * 1024);
		nsc.setRecieveBuffSize(128* 1024);
		nsc.setSentBuffSize(128 * 1024);
		NioSocketClient client = new NioSocketClient(nsc);
		
		int tcpSize = 1;
		for(int i = 0 ; i < tcpSize; i++){
			IoHandler handler1 = new CodecHandler(10000 / tcpSize, 64 * 1024, new BytesProtocol(), new TestAsyncByteObjetClientHandler());
			
			IoHandler handler = new MultiThreadHandler(3, 300, 60, TimeUnit.SECONDS,handler1);
//			
			
			
			
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler,"aaa" + i);
//			client.connect(new InetSocketAddress("192.168.2.131",1234),handler,"aaa" + i );
			client.connect(new InetSocketAddress("127.0.0.1",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.101.106.91",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.97.240.141",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.35.16",1234), handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.128.11",1234),handler,"aaa" + i );
			
		}
	}

}

class TestAsyncByteObjetClientHandler implements IoHandler{
//	public final static Logger logger = Logger.getLogger(TestAsyncByteObjetClientHandler.class);
//	private static AtomicInteger flag = new AtomicInteger(0);
//	private int i = 0;
	
	private byte[] sample = new byte[128];
	
	private final static int MAX = 100000;
	
	public CountDownLatch send(Object o,IoSession session) throws SessionHavaClosedException{
			int size = 0;
			CountDownLatch c = new CountDownLatch(1);
			synchronized (session) {
				LinkedList<CountDownLatch> l = (LinkedList<CountDownLatch>)session.attachment();
				synchronized (l) {
					while(l.size() > MAX){
						try {
							l.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					l.addLast(c);
				}
				session.write(o,false);
			}	
			return c;
	}


	
	@Override
	public void onSessionOpened(final IoSession session) {
		System.out.println("open");
		session.attach(new LinkedList<CountDownLatch>());
		for(int i = 0 ; i < 1 ; i++){
		Thread t = new Thread(){
			public void run(){
				int i = 0;
				while(true){
//				for(int j = 0 ; j < 10000 ; j ++){
					try {
//						Thread.sleep(1000);
						long ct = System.currentTimeMillis();
						CountDownLatch c = send(sample, session);
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
	public void onSessionClose(IoSession session) {
		System.out.println("close" + session);
	}
	private int cc = 0 ;
	private long ct = System.currentTimeMillis();
	private ReentrantLock lock = new ReentrantLock();
	private long start = System.currentTimeMillis();
	private long tc = 0;
	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
//		System.out.println(o);
//		try {
//			IoBuffer[] buffa = protocol.encode(buffPool, "ÄãºÃ£¡Mr server ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc                                                                                                                                                                                                                                                    !This is client" + attachment.customAttachment + "!write package" + (++i));
//			session.write(buffa);
//	} catch (SessionHavaClosedException e) {
//		e.printStackTrace();
//	}
		
			LinkedList<CountDownLatch> l = (LinkedList<CountDownLatch>)session.attachment();
		synchronized (l) {
			l.removeFirst().countDown();
			if(l.size() <= MAX){
				l.notify();
			}
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
	public void onMessageSent(IoSession session, int sendSize) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void catchException(IoSession session, Throwable e) {
		e.printStackTrace();
		
	}

	
}