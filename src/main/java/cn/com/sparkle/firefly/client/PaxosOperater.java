package cn.com.sparkle.firefly.client;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.com.sparkle.firefly.model.AddRequest.CommandType;

public interface PaxosOperater {

	public static interface CallBack {
		public void callBack(byte[] response);
	}

	/**
	 * wait all operation in this operation to finish
	 * 
	 * @param timeout
	 * @throws InterruptedException
	 * @throws MasterMayBeLostException
	 */
	public void waitAllFinish(long timeout) throws InterruptedException, MasterMayBeLostException;

	/**
	 * This is an async method with default time of timeout .
	 * 
	 * @throws InterruptedException
	 * @throws MasterMayBeLostException
	 */
	public Future<byte[]> add(byte[] value, CommandType commandType) throws InterruptedException, MasterMayBeLostException;

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
			MasterMayBeLostException;

	public Future<byte[]> add(byte[] value, long timeout, CommandType commandType, long instanceId, CallBack customCallback) throws InterruptedException,
			MasterMayBeLostException ;
	/**
	 * This is an sync method
	 * 
	 * @param value
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws MasterMayBeLostException
	 */
	public byte[] syncAdd(byte[] value, CommandType commandType) throws InterruptedException, TimeoutException, MasterMayBeLostException ;
	public byte[] syncAdd(byte[] value, CommandType commandType, int timeout, TimeUnit unit) throws InterruptedException, MasterMayBeLostException,
			TimeoutException ;
	public CommandAsyncProcessor getProcessor();
	public long getResponseInstanceId();
}
