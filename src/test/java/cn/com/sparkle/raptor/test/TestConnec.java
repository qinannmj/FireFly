package cn.com.sparkle.raptor.test;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cn.com.sparkle.raptor.core.buff.AllocateBytesBuff;
import cn.com.sparkle.raptor.core.buff.CycleBuff;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.buff.QuoteBytesBuff;
import cn.com.sparkle.raptor.core.handler.IoHandler;
import cn.com.sparkle.raptor.core.transport.socket.nio.IoSession;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketClient;
import cn.com.sparkle.raptor.core.transport.socket.nio.NioSocketConfigure;
import cn.com.sparkle.raptor.core.transport.socket.nio.exception.SessionHavaClosedException;

public class TestConnec {
	public static void main(String[] args) throws Exception {
		NioSocketConfigure nsc = new NioSocketConfigure();
		nsc.setTcpNoDelay(true);
		nsc.setSoTimeOut(1000*30);
		nsc.setReuseAddress(true);
		NioSocketClient client = new NioSocketClient(nsc);
		for(int i = 0 ; i < 10 ; i++){
			Thread t = new TestConnectThread(client);
			t.start();
		}
	}
}
class TestConnectThread extends Thread{
	NioSocketClient nc;
	public TestConnectThread(NioSocketClient nc){
		this.nc = nc;
	}
	public void run(){
		try{
			for(;;){
				Future<Boolean> f = nc.connect(new InetSocketAddress("127.0.0.1",1234), new TestClientHandler1());
//				nc.connect(new InetSocketAddress("192.168.3.100",1234), new TestClientHandler());
//				nc.connect(new InetSocketAddress("10.10.83.243",1234), new TestClientHandler());
//				client.connect(new InetSocketAddress("220.181.118.141",1234), new FilterChain(new TestClientHandler()));
				f.get();
//				client.connect(new InetSocketAddress("127.0.0.1",1234), new FilterChain(new TestClientHandler()));
				
			}
		}catch(Exception e){
//			e.printStackTrace();
		}
	}
}
class TestClientHandler1 implements IoHandler {
	public static AtomicInteger i = new AtomicInteger(0);
	public static long time = System.currentTimeMillis();
	private ReentrantLock lock = new ReentrantLock();
	long ct = System.currentTimeMillis();
	int cc = 0;
	@Override
	public void onMessageRecieved(IoSession session, Object message) {
	}

	@Override
	public void onMessageSent(IoSession session, int size) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onSessionClose(IoSession session) {
		// TODO Auto-generated method stub
//		System.out.println("session closed!!!");
	}
	
	
	private byte[] buff = new byte[128];
	@Override
	public void onSessionOpened(IoSession session) {
		int temp = i.addAndGet(1);
		if(temp % 100 ==0){
			System.out.println("connected:" + temp + " cost:" + (System.currentTimeMillis() - time));
		}
//		session.closeSession();
	}

	@Override
	public void catchException(IoSession session, Throwable e) {
		// TODO Auto-generated method stub
//		e.printStackTrace();
	}
}