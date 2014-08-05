package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.ProtocolHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestServerByteProtocol {
	public final static Logger logger = Logger.getLogger(TestServerByteProtocol.class);
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setProcessorNum(1);
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setTcpNoDelay(true);
//		nsc.setRecieveBuffSize(32* 1024);
		nsc.setSentBuffSize( 128* 1024);
		System.out.println("ffffffffffffffffffffffffff");
		//nsc.setRevieveBuffSize(1024 * 2048);
		//nsc.setTcpNoDelay(true);
		NioSocketServer server = new NioSocketServer(nsc);
		
		server.bind(new InetSocketAddress(1234),new MultiThreadProtecolHandler(1000, 32 * 1024, 2, 300, 60, TimeUnit.SECONDS,new ByteProtocol(128), new TestByteProtocolHandler()));
//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
//		TestAynscClientByteProtocol.main(args);
	}
	
}
class TestByteProtocolHandler implements ProtocolHandler{
	private int i = 0;
	

	@Override
	public void onOneThreadMessageRecieved(Object receiveObject,
			ProtocolHandlerIoSession session) {
		long ct = System.currentTimeMillis();
		try {
			session.writeObject("1");
		} catch (SessionHavaClosedException e) {
		}
		if(System.currentTimeMillis() - ct > 1000){
			TestServerByteProtocol.logger.debug("more than 1000 ms");
//			System.exit(0);
		}
//		try {
//			Thread.sleep(10);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
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
