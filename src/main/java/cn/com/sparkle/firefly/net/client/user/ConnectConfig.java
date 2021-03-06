package cn.com.sparkle.firefly.net.client.user;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.future.SystemFuture;
import cn.com.sparkle.firefly.net.client.NetNode;

public class ConnectConfig {
	private final static Logger logger = Logger.getLogger(ConnectConfig.class);

	private String address;
	private volatile boolean isAutoReConnect;
	private volatile NetNode node;
	private int masterDistance;
	private ConnectEvent connectEvent;
	private volatile SystemFuture<Boolean> future = new SystemFuture<Boolean>();

	private volatile SystemFuture<Boolean> handFuture = new SystemFuture<Boolean>();

	public static interface ConnectEvent {
		public void connect(String address, NetNode node);

		public void disconnect(String address, NetNode node);
	}

	public ConnectConfig(String address, boolean isAutoReConnect, int masterDistance, ConnectEvent connectEvent) {
		super();
		this.address = address;
		this.isAutoReConnect = isAutoReConnect;
		this.masterDistance = masterDistance;
		this.connectEvent = connectEvent;
	}
	
	public void reset(){
		future = new SystemFuture<Boolean>();
		handFuture = new SystemFuture<Boolean>();
	}

	public int getMasterDistance() {
		return masterDistance;
	}

	public void setMasterDistance(int masterDistance) {
		this.masterDistance = masterDistance;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isAutoReConnect() {
		return isAutoReConnect;
	}

	public void setAutoReConnect(boolean isAutoReConnect) {
		this.isAutoReConnect = isAutoReConnect;
	}

	public synchronized void connected(NetNode node) {
		this.node = node;
		connectEvent.connect(address, node);
		future.set(true);
	}

	public void disconnected(NetNode node) {
		connectEvent.disconnect(address, node);
		future.set(false);
		handFuture.set(false);
	}
	
	
	public void onRefused(){
		future.set(false);
		handFuture.set(false);
	}
	public synchronized NetNode getNode(){
		SystemFuture<Boolean> localHandFuture = handFuture;
		if(localHandFuture.isDone() && localHandFuture.value()){
			return node;
		}else return null;
	}
	
	public void handed() {
		handFuture.set(true);
	}

	/**
	 * 
	 * @return if connect success
	 * @throws InterruptedException
	 * @throws  
	 */
	public boolean waitOnOpenEvent() throws InterruptedException {
		try {
			waitOnOpenEvent(-1);
		} catch (TimeoutException e) {
			//the exception can't happen.
			logger.error("fatal error", e);
			System.exit(1);
		}
		return false;
	}

	public boolean waitOnOpenEvent(long timeoutMillis) throws InterruptedException, TimeoutException {
		if (timeoutMillis < 0) {
			return future.get();
		} else {
			return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		}
	}

	public boolean waitHanded(long timeoutMillis) throws InterruptedException, TimeoutException {
		if (timeoutMillis < 0) {
			return handFuture.get();
		} else {
			return handFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
		}
	}

	public boolean waitHanded() throws InterruptedException {
		try {
			waitHanded(-1);
		} catch (TimeoutException e) {
			//the exception can't happen.
			logger.error("fatal error", e);
			System.exit(1);
		}
		return false;
	}
}
