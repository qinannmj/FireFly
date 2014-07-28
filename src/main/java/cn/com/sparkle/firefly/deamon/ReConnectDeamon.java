package cn.com.sparkle.firefly.deamon;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;

public class ReConnectDeamon implements Runnable {

	private final static Logger logger = Logger.getLogger(ReConnectDeamon.class);

	public static interface ReConnectMethod {
		public void reConnect(Object value);
	}

	private static class WaitPackage implements Comparable<WaitPackage> {
		private long time;
		private ReConnectMethod method;
		private Object o;

		public WaitPackage(long delay, ReConnectMethod method, Object o) {
			super();
			this.time = System.currentTimeMillis() + delay;
			this.method = method;
			this.o = o;
		}

		@Override
		public int compareTo(WaitPackage o) {
			if (time == o.time)
				return 0;
			else if (time > o.time)
				return 1;
			else
				return -1;
		}
	}

	private PriorityBlockingQueue<WaitPackage> waitQueue = new PriorityBlockingQueue<ReConnectDeamon.WaitPackage>();

	public void startThread() {
		Thread t = new Thread(this);
		t.setName("reconnectDeamon");
		t.start();
	}

	public void add(Object o, ReConnectMethod method, int delay) {
		waitQueue.offer(new WaitPackage(delay, method, o));
	}

	public void run() {
		try {
			while (true) {
				Thread.sleep(100);
				long time = System.currentTimeMillis();
				while (waitQueue.peek() != null && time >= waitQueue.peek().time) {
					WaitPackage p = waitQueue.poll();
					p.method.reConnect(p.o);
				}
			}
		} catch (InterruptedException e) {
			logger.error("fatal error");
		}
	}
}
