package cn.com.sparkle.raptor.test;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.protocol.javaobject.ObjectProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.test.model.javaserialize.TestMessage;

public class TestAynscClientObjectProtocol {
	private final static Logger logger = Logger.getLogger(TestAynscClientObjectProtocol.class);
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setTcpNoDelay(true);
		nsc.setProcessorNum(1);
		nsc.setCycleRecieveBuffCellSize(1000);
		
		NioSocketClient client = new NioSocketClient(nsc);
		
//		nsc.setRecieveBuffSize(8 * 1024);
		IoHandler handler = new MultiThreadProtecolHandler(1000,  8 * 1024, 20, 300, 60, TimeUnit.SECONDS,new ObjectProtocol(), new TestAsyncProtocolObjetClientHandler());
		for(int i = 0 ; i < 1; i++){
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler,"aaa" + i);
//			client.connect(new InetSocketAddress("192.168.3.100",1234),handler,"aaa" + i );
			client.connect(new InetSocketAddress("127.0.0.1",1234),handler,"aaa" + i );
//			client.connect(new InetSocketAddress("10.232.128.11",1234),handler,"aaa" + i );
			
		}
		logger.warn("sssssss");
	}

}

class TestAsyncProtocolObjetClientHandler implements ProtocolHandler{
	
	private static AtomicInteger flag = new AtomicInteger(0);
//	private int i = 0;
	private String soure = "��ã�Mr server !This is client  ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";
	private String test = "";
	public TestAsyncProtocolObjetClientHandler(){
		for(int i = 0 ; i < 1 ;i++){
			test += soure;
		}
//		test = "a";
	}
	
	private LinkedList<CountDownLatch> l = new LinkedList<CountDownLatch>();
	private ReentrantLock llock = new ReentrantLock();
	public CountDownLatch send(Object o,ProtocolHandlerIoSession session) throws SessionHavaClosedException{
		CountDownLatch c;
		try{
			llock.lock();
			c = new CountDownLatch(1);
			
			l.addLast(c);
		}finally{
			llock.unlock();
		}
		int size = session.writeObject(o);
		return c;
	}
	@Override
	public void onOneThreadSessionOpen(final ProtocolHandlerIoSession session) {
		for(int i = 0 ; i < 1; i++){
		Thread t = new Thread(){
			public void run(){
				int i = 0;
				long now = System.currentTimeMillis();
				TestMessage tm = new TestMessage(-1,("��������" + ct + "   avvvasddwwq"),false);
				while(true){
//					IoBuffer[] buffa = protocol.encode(buffPool, "��ã�Mr server cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc!This is client" + attachment.customAttachment + "!write package" + (++i));
//					System.out.println("cellsize" + buffPool.getCellCapacity());
//					System.out.println(buffa[0].getByteBuffer().capacity() - buffa[0].getByteBuffer().remaining());
					
					try {
//						CountDownLatch c = send(test, session);
						CountDownLatch c = send(tm,session);
//						System.out.println("write object");
//						c.await();
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
					
				}
			}
		};
		t.start();
		}
//		IoBuffer[] buffa = protocol.encode(buffPool, "��ã�Mr server ccccccccccccccc!This is client" + attachment.customAttachment + "!write package" + (++i));
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
//			IoBuffer[] buffa = protocol.encode(buffPool, "��ã�Mr server ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc                                                                                                                                                                                                                                                    !This is client" + attachment.customAttachment + "!write package" + (++i));
//			session.write(buffa);
//	} catch (SessionHavaClosedException e) {
//		e.printStackTrace();
//	}
		try{
			llock.lock();
			l.removeFirst().countDown();
//			if(l.size() != 0){
//				System.out.println("11111111111");
//			}
		}finally{
			llock.unlock();
		}
		try{
			lock.lock();
			++cc;
			++tc;
			if(cc%1000000 == 0){
				long tt = System.currentTimeMillis() - ct;
				System.out.println((cc*1000/tt) + "/s   " + (tc /(System.currentTimeMillis() - start) * 1000) + "/s");
				ct = System.currentTimeMillis();
				cc = 1;
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
		
	}

	
}