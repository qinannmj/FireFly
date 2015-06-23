package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.protobuf.ProtoBufProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage.Person;

import com.sun.corba.se.pept.protocol.ProtocolHandler;

public class TestAynscClientProtobufProtocol {
	public final static Logger logger = Logger.getLogger(TestAynscClientProtobufProtocol.class);
	
	public static void main(String[] args) throws Exception {
		logger.debug("start");
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setTcpNoDelay(true);
		nsc.setProcessorNum(1);
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setSoTimeOut(10);
		nsc.setRecieveBuffSize(128 * 1024);
		nsc.setSentBuffSize(128 * 1024);
//		nsc.setReuseAddress(true);
		
		NioSocketClient client = new NioSocketClient(nsc);
		ProtoBufProtocol protocol = new ProtoBufProtocol(PersonMessage.Person.getDefaultInstance());
//		ProtoBufProtocol protocol = new ProtoBufProtocol();
//		protocol.registerMessage(1, PersonMessage.Person.getDefaultInstance());
		
		TestAynscClientProtobufProtocolHandler ih = new TestAynscClientProtobufProtocolHandler();
		IoHandler handler = new MultiThreadHandler( 20, 300, 60, TimeUnit.SECONDS,new CodecHandler(1024, 64 * 1024, protocol, ih)  );
		for(int i = 0 ; i < 1 ; i++){
//		while(true){
			WaitFinishConnect wfc = new WaitFinishConnect();
//			client.connect(new InetSocketAddress("10.10.83.243",1234), handler,"aaa" + i);
//			client.connect(new InetSocketAddress("192.168.3.100",1234),handler,"aaa" + i );
			
//			client.connect(new InetSocketAddress("10.238.130.23",1234),handler, wfc);
			client.connect(new InetSocketAddress("127.0.0.1",1234),handler, wfc).get();
//			client.connect(new InetSocketAddress("10.32.80.85",1234),handler, wfc).get();
//			client.connect(new InetSocketAddress("10.232.133.72", 10011), handler);
//			client.connect(new InetSocketAddress("10.232.35.16",1234), handler,wfc);	
//			client.connect(new InetSocketAddress("10.232.128.11",1234),handler,"aaa" + i );
//			wfc.count.await();
//			Person.Builder builder = Person.newBuilder().setId(1).setName(ih.soure);
//			AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
//			CountDownLatch c = ih.send(ab.build(),wfc.session);
//			c.await();
//			wfc.session.closeSocketChannel();
		}
	}

}
class WaitFinishConnect{
	CountDownLatch count = new CountDownLatch(1);
	IoSession session;
}
class TestAynscClientProtobufProtocolHandler implements IoHandler{
	private static AtomicInteger flag = new AtomicInteger(0);
	public String origin = "ÄãºÃ£¡Mr server !This is client  cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";
	public String soure = "";
	
	public TestAynscClientProtobufProtocolHandler(){
		for(int i = 0 ; i < 1 ; i++){
			soure += origin;
		}
	}
	
	private LinkedList<CountDownLatch> l = new LinkedList<CountDownLatch>();
	private ReentrantLock llock = new ReentrantLock();
	public CountDownLatch send(Object o,IoSession session) throws SessionHavaClosedException{
		CountDownLatch c;
		try{
			llock.lock();
			c = new CountDownLatch(1);
			
			l.addLast(c);
		}finally{
			llock.unlock();
		}
		session.write(o, false);
		return c;
	}
	@Override
	public void onSessionOpened(final IoSession session) {
		WaitFinishConnect wfc = (WaitFinishConnect)session.attachment();
		wfc.session = session;
		wfc.count.countDown();
		Person.Builder builder = Person.newBuilder().setId(0).setName(soure);
//		AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
		System.out.println("size" + builder.build().getSerializedSize());
		
		for(int i = 0 ; i < 1; i++){
		Thread t = new Thread(){
			public void run(){
				int i = 0;
				long now = System.currentTimeMillis();
//				for(int j = 0 ; j<1 ; ++j){
				while(true){
					try {
						Person.Builder builder = Person.newBuilder().setId(++i).setName(soure);
//						AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
						CountDownLatch c = send(builder.build(),session);
						c.await();
//						break;
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
	public void onSessionClose(IoSession session) {
		System.out.println("close" + session.attachment());
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
		Person p = (Person)message;
//		System.out.println(p.getId());
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
			if(cc%100000 == 0){
				long tt = System.currentTimeMillis() - ct;
				System.out.println((cc*1000/tt) + "/s   " + (tc*1000 /(System.currentTimeMillis() - start)) + "/s");
				ct = System.currentTimeMillis();
				cc = 1;
			}
		}finally{
			lock.unlock();
		}
//		session.closeSession();
	}
	@Override
	public void catchException(IoSession session, Throwable e) {
		TestAynscClientProtobufProtocol.logger.error("", e);
	}
	@Override
	public void onMessageSent(IoSession session, int sendSize) {
	}

	
}