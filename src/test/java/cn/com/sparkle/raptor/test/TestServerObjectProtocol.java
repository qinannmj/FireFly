package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.protocol.javaobject.ObjectProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestServerObjectProtocol {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QueueFullException 
	 */
	public static void main(String[] args) throws IOException, QueueFullException {
		// TODO Auto-generated method stub
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(1);
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setTcpNoDelay(true);
//		nsc.setRecieveBuffSize(32* 1024);
//		nsc.setSentBuffSize( 8 * 1024);
		//nsc.setRevieveBuffSize(1024 * 2048);
		//nsc.setTcpNoDelay(true);
		NioSocketServer server = new NioSocketServer(nsc);
		server.bind(new InetSocketAddress(1234),new MultiThreadProtecolHandler(20000, 512, 20, 300, 60, TimeUnit.SECONDS,new ObjectProtocol(), new TestObjectProtocolHandler()));
//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
	}
	
}
class TestObjectProtocolHandler implements ProtocolHandler{
	private int i = 0;
	

	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
		try {
			session.writeObject( "ÄãºÃ£¡Mr client!This is server!" + (++i));
//			session.writeObject("ÄãºÃ£¡");
		} catch (SessionHavaClosedException e) {
		}
	}


	@Override
	public void onOneThreadSessionOpen(ProtocolHandlerIoSession session) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onOneThreadSessionClose(ProtocolHandlerIoSession session) {
		System.out.println("disconnect");
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
