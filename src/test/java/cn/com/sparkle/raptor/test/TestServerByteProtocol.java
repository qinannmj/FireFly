package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.bytes.BytesProtocol;
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
		nsc.setProcessorNum(32);
		nsc.setCycleRecieveBuffCellSize(1000);
		nsc.setTcpNoDelay(true);
		nsc.setRecieveBuffSize(128* 1024);
		nsc.setSentBuffSize( 128* 1024);
		System.out.println("bbbbbbbbbbbbbbbbbb");
		//nsc.setRevieveBuffSize(1024 * 2048);
		//nsc.setTcpNoDelay(true);
		NioSocketServer server = new NioSocketServer(nsc);
		server.bind(new InetSocketAddress(1234),new MultiThreadHandler(2, 300, 60, TimeUnit.SECONDS,new CodecHandler(1000, 32 * 1024, new BytesProtocol(), new TestByteProtocolHandler())));
		
//		server.bind(new InetSocketAddress(1234),new CodecHandler(1000, 64 * 1024, new BytesProtocol(), new TestByteProtocolHandler()));
//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
//		TestAynscClientByteProtocol.main(args);
	}
	
}
class TestByteProtocolHandler implements IoHandler{
	
	byte[] response = new byte[128];


	@Override
	public void onSessionOpened(IoSession session) {
		// TODO Auto-generated method stub
		System.out.println("server open session");
	}


	@Override
	public void onSessionClose(IoSession session) {
		System.out.println("disconnect");
		
	}


	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
		long ct = System.currentTimeMillis();
		try {
			session.write(response,false);
		} catch (SessionHavaClosedException e) {
		}
		if(System.currentTimeMillis() - ct > 1000){
			TestServerByteProtocol.logger.debug("more than 1000 ms");
//			System.exit(0);
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
