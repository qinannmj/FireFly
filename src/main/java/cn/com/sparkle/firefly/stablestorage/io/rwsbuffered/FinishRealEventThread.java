package cn.com.sparkle.firefly.stablestorage.io.rwsbuffered;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class FinishRealEventThread implements Runnable {
	private final static Logger logger = Logger.getLogger(FinishRealEventThread.class);
	private LinkedBlockingQueue<List<Callable<Object>>> queue = new LinkedBlockingQueue<List<Callable<Object>>>();
	
	public void addFinishEvent(List<Callable<Object>> list) {
		try {
			queue.put(list);
		} catch (InterruptedException e) {
			logger.error("fatal error", e);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				List<Callable<Object>> callList = queue.take();
				for(Callable<Object> call : callList){
					call.call();
				}
			} catch (Exception e) {
				logger.error("fatal error", e);
			}
		}

	}
}
