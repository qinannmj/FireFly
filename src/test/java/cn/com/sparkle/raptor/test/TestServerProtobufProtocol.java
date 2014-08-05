package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.protocol.javaobject.ObjectProtocol;
import cn.com.sparkle.raptor.core.protocol.protobuf.ProtoBufProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage.AddressBook;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage.Person;

public class TestServerProtobufProtocol {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QueueFullException 
	 */
	public static void main(String[] args) throws IOException, QueueFullException {
		// TODO Auto-generated method stub
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(2);
		nsc.setCycleRecieveBuffCellSize(10);
		nsc.setTcpNoDelay(true);
		nsc.setReuseAddress(true);
//		nsc.setRecieveBuffSize(32* 1024);
//		nsc.setSentBuffSize( 8 * 1024);
		//nsc.setRevieveBuffSize(1024 * 2048);
		//nsc.setTcpNoDelay(true);
		
		ProtoBufProtocol protocol = new ProtoBufProtocol(PersonMessage.Person.getDefaultInstance());
		
//		ProtoBufProtocol protocol = new ProtoBufProtocol();
//		protocol.registerMessage(1, PersonMessage.Person.getDefaultInstance());
		NioSocketServer server = new NioSocketServer(nsc);
		server.bind(new InetSocketAddress(1234),new MultiThreadProtecolHandler(1000,64 * 1024, 20, 300, 60, TimeUnit.SECONDS,protocol, new ProtobufProtocolHandler()));
//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
	}
	
}
class ProtobufProtocolHandler implements ProtocolHandler{
	private int i = 0;
	private String soure = "";
	public String origin = "ÄãºÃ£¡Mr server !This is client  cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";
	public ProtobufProtocolHandler(){
		for(int i = 0 ; i < 1 ; i++){
			soure += origin;
		}
		Person.Builder builder = Person.newBuilder().setId(2).setName(soure);
//		AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
//		System.out.println("size:" + builder.build().getSerializedSize());
	}

	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
		try {
			Person p = (Person)receiveObject;
//			System.out.println(p.getId());
			
			Person.Builder builder = Person.newBuilder().setId(p.getId()).setName(p.getName());
//			AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
			
			long ct = System.currentTimeMillis();
			session.writeObject( builder.build() );
			if(System.currentTimeMillis() - ct > 1000){
				TestServerByteProtocol.logger.debug("more than 1000 ms");
//				System.exit(0);
			}
//			try {
//				Thread.sleep(1);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			session.writeObject("ÄãºÃ£¡");
		} catch (SessionHavaClosedException e) {
		}
	}


	@Override
	public void onOneThreadSessionOpen(ProtocolHandlerIoSession session) {
		// TODO Auto-generated method stub
		
	}

	AtomicInteger ai = new AtomicInteger();
	@Override
	public void onOneThreadSessionClose(ProtocolHandlerIoSession session) {
		System.out.println("disconnect" + ai.addAndGet(1));
	}


	@Override
	public void onOneThreadCatchException(IoSession ioSession,
			ProtocolHandlerIoSession attachment, Throwable e) {
		e.printStackTrace();
		
	}




	@Override
	public void onOneThreadMessageSent(ProtocolHandlerIoSession session,int sendSize) {
		// TODO Auto-generated method stub
		
	}


	
	
}
