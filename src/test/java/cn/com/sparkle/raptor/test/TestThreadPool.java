package cn.com.sparkle.raptor.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestThreadPool {
	public static void main(String[] args) {
		final ThreadPoolExecutor tp = new ThreadPoolExecutor(10, 10, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		Thread t = new Thread(){
			public void run(){
				int i = 0;
				long time = System.currentTimeMillis();
				while(true){
					final CountDownLatch c = new CountDownLatch(1);
					tp.execute(new Runnable() {
						@Override
						public void run() {
							c.countDown();
						}
					});
					try {
						c.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(i++ == 100000){
						long t = System.currentTimeMillis();
						System.out.println(i * 1000 / (t - time));
						time = t;
						i = 0;
					}
				}
			}
		};
		t.start();
	}
}
