package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class FinishRealEventThread implements Runnable {
	private final static Logger logger = Logger.getLogger(FinishRealEventThread.class);
	private LinkedBlockingQueue<Callable<Object>> queue = new LinkedBlockingQueue<Callable<Object>>();
	
	public void addFinishEvent(Callable<Object> f) {
		try {
			queue.put(f);
		} catch (InterruptedException e) {
			logger.error("fatal error", e);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Callable<Object> call = queue.take();
				call.call();
			} catch (Exception e) {
				logger.error("fatal error", e);
			}
		}

	}
}
