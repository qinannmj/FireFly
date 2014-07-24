package cn.com.sparkle.paxos.future;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

public class SystemFuture<T> extends FutureTask<T> {
	private final static Logger logger = Logger.getLogger(SystemFuture.class);
	@SuppressWarnings("rawtypes")
	private static Callable nullCallable = new Callable() {
		@Override
		public Object call() throws Exception {
			return null;
		}
	};

	@SuppressWarnings("unchecked")
	public SystemFuture() {
		super(nullCallable);
	}

	@Override
	public void set(T v) {
		if (!isDone()) {
			super.set(v);
		}
	}

	@Override
	public T get() throws InterruptedException {
		try {
			return super.get();
		} catch (ExecutionException e) {
			//the exception can't happen.
			logger.error("fatal error", e);
			System.exit(1);
		}
		return null;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		try {
			return super.get(timeout, unit);
		} catch (ExecutionException e) {
			//the exception can't happen.
			logger.error("fatal error", e);
			System.exit(1);
		}
		return null;
	}
}
