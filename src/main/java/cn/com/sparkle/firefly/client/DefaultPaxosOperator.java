package cn.com.sparkle.firefly.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.client.PaxosClient.CommandCallBack;
import cn.com.sparkle.firefly.future.SystemFuture;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;

public class DefaultPaxosOperator implements PaxosOperater{
	private final static int DEFULT_ADD_TIMEOUT = 5000;
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(PaxosOperater.class);

	private CommandAsyncProcessor processor;
	private AtomicInteger waitFinishCounter = new AtomicInteger(0);
	private ReentrantLock waitLock = new ReentrantLock();
	private Condition allDoCondition = waitLock.newCondition();
	private boolean isWaitFinish = false;

	private AtomicLong responseInstanceId;

	private class ASyncCommandCallBack implements CommandCallBack {
		private SystemFuture<byte[]> future;
		private CallBack customCallback;

		public ASyncCommandCallBack(SystemFuture<byte[]> future, CallBack customCallback) {
			super();
			this.future = future;
			this.customCallback = customCallback;
		}

		@Override
		public void response(byte[] response, long instanceId) {
			future.set(response);
			waitFinishCounter.decrementAndGet();
			try {
				waitLock.lock();
				while(true){
					long old = responseInstanceId.get();
					if (instanceId > old) {
						if(!responseInstanceId.compareAndSet(old, instanceId)){
							continue;
						}
					}
					break;
				}
				if (isWaitFinish && waitFinishCounter.get() == 0) {
					allDoCondition.signal();
					isWaitFinish = false;
				}
				if (this.customCallback != null) {

					try {
						this.customCallback.callBack(response);
					} catch (com.google.protobuf.UninitializedMessageException e) {
						throw e;
					}
				}
			} finally {
				waitLock.unlock();
			}

		}
	}

	public DefaultPaxosOperator(CommandAsyncProcessor processor,AtomicLong responseInstanceId) {
		super();
		this.responseInstanceId = responseInstanceId;
		this.processor = processor;
	}

	/**
	 * wait all operation in this operation to finish
	 * 
	 * @param timeout
	 * @throws InterruptedException
	 * @throws MasterMayBeLostException
	 */
	public void waitAllFinish(long timeout) throws InterruptedException, MasterMayBeLostException {
		try {
			waitLock.lock();
			isWaitFinish = true;
			if (waitFinishCounter.get() != 0) {
				if (timeout == 0) {
					allDoCondition.await();
				} else {
					allDoCondition.await(timeout, TimeUnit.MILLISECONDS);
				}
			}
			isWaitFinish = waitFinishCounter.get() != 0;
			if (isWaitFinish) {
				throw new MasterMayBeLostException();
			}

		} finally {
			waitLock.unlock();
		}
	}

	/**
	 * This is an async method with default time of timeout .
	 * 
	 * @throws InterruptedException
	 * @throws MasterMayBeLostException
	 */
	public Future<byte[]> add(byte[] value, CommandType commandType) throws InterruptedException, MasterMayBeLostException {
		return add(value, DEFULT_ADD_TIMEOUT, commandType, null);
	}

	/**
	 * 
	 * @param value
	 * @param timeout
	 * @param isTransportMaster
	 * @param customCallback
	 * @return
	 * @throws InterruptedException
	 * @throws MasterMayBeLostException
	 */
	public Future<byte[]> add(byte[] value, long timeout, CommandType commandType, CallBack customCallback) throws InterruptedException,
			MasterMayBeLostException {
		return add(value, timeout, commandType, commandType.isConsistentlyRead() ? responseInstanceId.get() : -1, customCallback);
	}

	public Future<byte[]> add(byte[] value, long timeout, CommandType commandType, long instanceId, CallBack customCallback) throws InterruptedException,
			MasterMayBeLostException {
		if (value == null) {
			throw new NullPointerException("the value is null");
		}
		SystemFuture<byte[]> systemFuture = new SystemFuture<byte[]>();
		CommandCallBack commandCallBack = new ASyncCommandCallBack(systemFuture, customCallback);
		waitFinishCounter.addAndGet(1);
		if (!processor.addCommand(new Command(commandType, instanceId, value, commandCallBack), timeout, TimeUnit.MILLISECONDS)) {
			waitFinishCounter.decrementAndGet();
			throw new MasterMayBeLostException();
		}
		return systemFuture;
	}

	/**
	 * This is an sync method
	 * 
	 * @param value
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws MasterMayBeLostException
	 */
	public byte[] syncAdd(byte[] value, CommandType commandType) throws InterruptedException, TimeoutException, MasterMayBeLostException {
		return syncAdd(value, commandType, DEFULT_ADD_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public byte[] syncAdd(byte[] value, CommandType commandType, int timeout, TimeUnit unit) throws InterruptedException, MasterMayBeLostException,
			TimeoutException {
		Future<byte[]> f = add(value, commandType);
		try {
			return f.get(timeout, unit);
		} catch (ExecutionException e) {
			throw new RuntimeException("fatal exception", e);
		}
	}

	public CommandAsyncProcessor getProcessor() {
		return processor;
	}

}
