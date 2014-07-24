package cn.com.sparkle.paxos.net.client;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.net.frame.FrameBody;
import cn.com.sparkle.paxos.net.netlayer.NetCloseException;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;

public abstract class NetNode implements Comparable<NetNode> {
	private final static Logger logger = Logger.getLogger(NetNode.class);

	private PaxosSession session;
	private AtomicLong packageId = new AtomicLong(0);
	private ConcurrentHashMap<Long, CallBack<? extends Object>> waitFinishMap = new ConcurrentHashMap<Long, CallBack<? extends Object>>(1024);
	ReentrantLock lock = new ReentrantLock();
	private String address;

	private Protocol protocol = null;

	private String appVersion;

	private int heartBeatInterval;

	public NetNode(PaxosSession session, String address, Protocol protocol, String appVersion, int heartBeatInterval) {
		this.session = session;
		this.address = address;
		this.protocol = protocol;
		this.appVersion = appVersion;
		this.heartBeatInterval = heartBeatInterval;
	}

	protected void write(byte[] message, long packageId, CallBack<? extends Object> callBack) throws NetCloseException {
		FrameBody body;
		try {
			body = new FrameBody(message, session.getChecksumType());
			if (callBack != null) {
				waitFinishMap.put(packageId, callBack);
			}
			try {
				this.session.write(body);
			} catch (NetCloseException e) {
				if (callBack != null) {
					callBack = waitFinishMap.remove(packageId);
					if (callBack != null) {
						callBack.fail(this);
					}
				}
			}
		} catch (UnsupportedChecksumAlgorithm e1) {
			logger.error("unexcepted error", e1);
		}
	}

	@SuppressWarnings("unchecked")
	public void recieve(long id, boolean isLast, Object o) {
		if (id == -1)
			return;
		CallBack<? extends Object> callBack = waitFinishMap.get(id);
		if (callBack != null) {
			((CallBack<Object>) callBack).call(this, o);
			if (isLast) {
				callBack = waitFinishMap.remove(id);
			}
		} else {
			logger.warn("callback is null,maybe there is a bug!");
		}
	}

	public void close() {
		session.closeSession();
	}

	protected void onClose() {
		Iterator<Entry<Long, CallBack<? extends Object>>> i = waitFinishMap.entrySet().iterator();
		while (i.hasNext()) {
			Entry<Long, CallBack<? extends Object>> e = i.next();
			CallBack<? extends Object> callback = waitFinishMap.remove(e.getKey());
			if (callback != null) {
				callback.fail(this);
			}
		}
	}

	public boolean isClose() {
		return session.isClose();
	}

	@Override
	public int compareTo(NetNode o) {
		return address.compareTo(o.address);
	}

	protected long generatePackageId() {
		long newVal;
		for (;;) {
			long oldVal = packageId.get();
			if (oldVal == Long.MAX_VALUE) {
				session.closeSession();
			}
			newVal = (oldVal + 1) % Long.MAX_VALUE;
			if (packageId.compareAndSet(oldVal, newVal)) {
				break;
			}
		}
		return newVal;
	}

	public PaxosSession getSession() {
		return session;
	}

	public String getAddress() {
		return address;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public int getHeartBeatInterval() {
		return heartBeatInterval;
	}

}
