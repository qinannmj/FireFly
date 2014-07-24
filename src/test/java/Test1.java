import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Test1 {
	static int i = 0;
	static long ct;
	final static ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1000);
	public final static int SIZE = 1000000;
	 static class TestThread implements Runnable{
		public void run(){
			++i;
			if(i%SIZE == 0){
				long cd = System.currentTimeMillis() - ct;
				System.out.println((SIZE / cd * 1000));
				i =0;
				ct = System.currentTimeMillis();
			}
		}
	}
	public static void main(String[] args) {
		ThreadPoolExecutor te =new ThreadPoolExecutor(1, 1,
				10000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
		ct = System.currentTimeMillis();
		
		Thread t = new Thread(){
			public void run(){
				while(true){
					try {
						queue.take().run();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
		while(true){
//			Future<?> f = te.submit(new TestThread());
			try{
				te.submit(new TestThread());
			}catch(Exception e){
				
			}
//			try {
//				f.get();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			queue.offer(new TestThread());
		}
	}
}
