package cn.com.sparkle.firefly.stablestorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.BufferedFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup;

public class TestBufferOut {
	private static int count = 0;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		FlushThreadGroup flushThreadGroup = new FlushThreadGroup(1024 * 1024 * 10, 20, "", false);
		//		FlushThreadGroup flushThreadGroup = new FlushThreadGroup(20, "", false);
		final AtomicInteger ai = new AtomicInteger();
		final BufferedFileOut out = new BufferedFileOut("d://jbpaxos//a.test1", new RandomAccessFile("d://jbpaxos//a.test1", "rws"), flushThreadGroup);
		for (int i = 0; i < 1; ++i) {
			Thread t = new Thread() {
				public void run() {
					byte[] buf = new byte[128 * 8 *1024];
					//									byte[] buf = new byte[1024 * 1024 * 8];

					for (int i = 0; i < 10000000; ++i) {
						final CountDownLatch c = new CountDownLatch(1);
						try {
							out.write(buf, 0, buf.length, new Callable<Object>() {
								@Override
								public Object call() throws Exception {
									c.countDown();
									ai.incrementAndGet();
									return null;
								}
							}, true);
							c.await();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			};
			t.start();
		}
		Thread calc = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int c = ai.getAndSet(0);
					System.out.println(c / 3);
				}
			}
		};
		calc.start();
	}
}
