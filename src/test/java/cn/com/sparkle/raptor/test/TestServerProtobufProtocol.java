package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.protobuf.ProtoBufProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;
import cn.com.sparkle.raptor.test.model.protocolbuffer.PersonMessage;
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
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setTcpNoDelay(true);
		nsc.setReuseAddress(true);
		nsc.setRecieveBuffSize(1024 * 1024);
		nsc.setSentBuffSize(1024 * 1024);
		//		nsc.setRecieveBuffSize(32* 1024);
		//		nsc.setSentBuffSize( 8 * 1024);
		//nsc.setRevieveBuffSize(1024 * 2048);
		//nsc.setTcpNoDelay(true);

		ProtoBufProtocol protocol = new ProtoBufProtocol(PersonMessage.Person.getDefaultInstance());

		//		ProtoBufProtocol protocol = new ProtoBufProtocol();
		//		protocol.registerMessage(1, PersonMessage.Person.getDefaultInstance());
		NioSocketServer server = new NioSocketServer(nsc);
		server.bind(new InetSocketAddress(1234), new MultiThreadHandler(20, 300, 60, TimeUnit.SECONDS, new CodecHandler(1000, 128 * 1024, protocol, new ProtobufProtocolHandler()) ));
		//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
	}

}

class ProtobufProtocolHandler implements IoHandler {
	private int i = 0;
	private String soure = "";
	public String origin = "��ã�Mr server !This is client  cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc             !write package";

	public ProtobufProtocolHandler() {
		for (int i = 0; i < 1; i++) {
			soure += origin;
		}
		Person.Builder builder = Person.newBuilder().setId(2).setName(soure);
		//		AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);
		//		System.out.println("size:" + builder.build().getSerializedSize());
	}

	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
		try {
			Person p = (Person) message;
			//			System.out.println(p.getId());

			Person.Builder builder = Person.newBuilder().setId(p.getId()).setName(p.getName());
			//			AddressBook.Builder ab = AddressBook.newBuilder().addPerson(builder);

			long ct = System.currentTimeMillis();
			session.write(builder.build(), false);
			if (System.currentTimeMillis() - ct > 1000) {
				TestServerByteProtocol.logger.debug("more than 1000 ms");
				//				System.exit(0);
			}
			//			try {
			//				Thread.sleep(1);
			//			} catch (InterruptedException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
			//			session.writeObject("��ã�");
		} catch (SessionHavaClosedException e) {
		}
	}

	@Override
	public void onSessionOpened(IoSession session) {

	}

	AtomicInteger ai = new AtomicInteger();

	@Override
	public void onSessionClose(IoSession session) {
		System.out.println("disconnect" + ai.addAndGet(1));
	}

	@Override
	public void catchException(IoSession ioSession, Throwable e) {
		e.printStackTrace();

	}

	@Override
	public void onMessageSent(IoSession session, int sendSize) {
		// TODO Auto-generated method stub

	}

}
