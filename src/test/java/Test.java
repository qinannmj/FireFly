import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
	private static ReentrantLock lock = new ReentrantLock();
	private static long ii = 25000000;
	private static long time;

	private static class It1 extends Thread {
		private int count = 0;
		public void run() {
			while (true) {
				try {
					lock.lock();
					if (ii == 0) {
						break;
					}
					--ii;
					count++;
					if (ii == 0) {
						System.out.println(System.currentTimeMillis() - time);
					}
				} finally {
					lock.unlock();
				}
			
			}
			System.out.println("count:" + count);
		}
	}

	static volatile byte turn;
	static byte[] canRun = new byte[2];
	static {
		for (int i = 0; i < canRun.length; i++)
			canRun[i] = 0;
	}
	
	private static class It2 extends Thread {
		private byte self;
		private byte opponent;
		private int count = 0;
		private static int maxSpinCount = 2000;
		public It2(byte self, byte opponent) {
			this.self = self;
			this.opponent = opponent;
		}

		public void run() {
			while (true) {
				canRun[self] = 1;
				turn = self;
				for(int i = 0 ;turn == self && canRun[opponent] == 1 ;i++){
//					System.out.println(self);
					if(i >= maxSpinCount){
						i = 0;
//						System.out.println(self + "   " + ii);
//						try {
//							Thread.sleep(1);
//						} catch (InterruptedException e) {
//						}
					}
				}
				try {
					if (ii == 0) {
						break;
					}
					--ii;
					count++;
					if (ii == 0) {
						System.out.println(System.currentTimeMillis() - time);
					}
				} finally {
					canRun[self] = 0;
				}
			}
			System.out.println("count:" + count +  "   ii" + ii);
		}
	}
	
	private static class It3 extends Thread {
		private int self;
		private static AtomicInteger lock = new AtomicInteger();
		
		private int count = 0;
		public It3(int self) {
			this.self = self;
		}

		public void run() {
			while (true) {
				
				try {
					while (!lock.compareAndSet(0, self));
					if (ii == 0) {
						break;
					}
					--ii;
					count++;
					if (ii == 0) {
						System.out.println(System.currentTimeMillis() - time);
					}
				} finally {
					lock.set(0);
				}
			}
			System.out.println("count:" + count);
		}
	}
	private static class It4 extends Thread{
		private static Object o = new Object();
		private int count = 0;
		public void run() {
			while (true) {
				synchronized (o) {
					if (ii == 0) {
						break;
					}
					--ii;
					count++;
					if (ii == 0) {
						System.out.println(System.currentTimeMillis() - time);
					}
				}
			}
			System.out.println("count:" + count);
		}
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		Test.time = System.currentTimeMillis();
//		 new It1().start();
//		 new It1().start();

		new It2((byte)0, (byte)1).start();
		new It2((byte)1, (byte)0).start();
//		new It3(1).start();
//		new It3(2).start();
		
//		new It4().start();
//		 new It4().start();
//		ByteBuffer b = ByteBuffer.allocate(1024);
//		System.out.println(b.remaining());
//		b.put((byte)8);
//		System.out.println(b.limit(b.position()));
//		b.rewind();
//		
//		System.out.println(b.remaining());
//		System.out.println(b.capacity());
//		System.out.println(b.rewind());
//		System.out.println(b.remaining());
//		System.out.println(b.capacity());
//		System.out.println(b.get());
//		String s = "中";
//		byte b1 = (byte) (s.charAt(0) >> 8);
//		byte b2 = (byte)(s.charAt(0) & 0xFF);
//		System.out.println((int)s.charAt(0));
//		System.out.println(b1);
//		System.out.println(b2);
//		System.out.println((char)(b1<<8 | b2));
//		
//		byte[] bs = s.getBytes("utf-8");
//		for(int i = 0 ; i < bs.length ; i++){
//			System.out.println(bs[i]);
//		}
	}
}
