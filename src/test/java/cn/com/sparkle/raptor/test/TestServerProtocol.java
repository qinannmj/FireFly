package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.protocol.CodecHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.CodecHandler;
import cn.com.sparkle.raptor.core.protocol.MultiThreadHandler;
import cn.com.sparkle.raptor.core.protocol.textline.TextLineProtocol;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketServer;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

import com.sun.corba.se.pept.protocol.ProtocolHandler;

public class TestServerProtocol {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QueueFullException 
	 */
	public static void main(String[] args) throws IOException, QueueFullException {
		// TODO Auto-generated method stub
		NioSocketConfigure nsc = new NioSocketConfigure();
		//nsc.setSentBuffSize(1024);
		//nsc.setRevieveBuffSize(1024 * 2048);
		nsc.setReuseAddress(true);
		nsc.setProcessorNum(16);
		nsc.setTcpNoDelay(true);
		NioSocketServer server = new NioSocketServer(nsc);
		IoHandler handler = new MultiThreadHandler(20, 300, 60, TimeUnit.SECONDS, new CodecHandler(100000, 1024, new TextLineProtocol(),
				new TestProtocolHandler()));
		server.bind(new InetSocketAddress(1234), handler);
		//		server.bind(new InetSocketAddress(12345),new FilterChain(new TestHandler()));
	}

}

class TestProtocolHandler implements IoHandler {
	private int i = 0;

	@Override
	public void onSessionOpened(IoSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionClose(IoSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageRecieved(IoSession session, Object message) throws IOException {
		//		System.out.println(recieveObject);
		try {
			session.write("ÄãºÃ£¡Mr client!This is server!" + (++i), false);
		} catch (SessionHavaClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void onMessageSent(IoSession session, int sendSize) {
		// TODO Auto-generated method stub

	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		// TODO Auto-generated method stub

	}

}
