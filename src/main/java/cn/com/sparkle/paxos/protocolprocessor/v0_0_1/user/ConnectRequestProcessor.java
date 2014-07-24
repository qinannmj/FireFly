package cn.com.sparkle.paxos.protocolprocessor.v0_0_1.user;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.listeners.MasterDistanceChangeListener;
import cn.com.sparkle.paxos.net.netlayer.AttributeKey;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.AbstractProtocolV0_0_1Processor;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.ConnectRequest;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.ConnectResponse;
import cn.com.sparkle.paxos.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;

public class ConnectRequestProcessor extends AbstractProtocolV0_0_1Processor implements MasterDistanceChangeListener {
	private Logger logger = Logger.getLogger(ConnectRequestProcessor.class);

	private final static AttributeKey<Integer> distanceKey = new AttributeKey<Integer>("sessionMasterDistance");

	private int masterDistance = Integer.MAX_VALUE;
	private ConcurrentHashMap<Integer, ConcurrentHashMap<PaxosSession, PaxosSession>> wantConnectMasterSessionMap = new ConcurrentHashMap<Integer, ConcurrentHashMap<PaxosSession, PaxosSession>>();
	private Configuration conf;
	private ReentrantLock lock = new ReentrantLock();
	private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	public ConnectRequestProcessor(Context context) {
		context.getEventsManager().registerListener(this);
		this.conf = context.getConfiguration();
	}

	@Override
	public void receive(MessagePackage t, PaxosSession session) throws InterruptedException {
		if (t.hasConnectRequest()) {
			ConnectRequest request = t.getConnectRequest();
			if (request.getMasterDistance() > masterDistance) {
				try {
					rwlock.readLock().lock();
					ConcurrentHashMap<PaxosSession, PaxosSession> set = wantConnectMasterSessionMap.get(request.getMasterDistance());
					if (set == null) {
						try {
							lock.lock();
							set = new ConcurrentHashMap<PaxosSession, PaxosSession>();
							wantConnectMasterSessionMap.put(request.getMasterDistance(), set);
						} finally {
							lock.unlock();
						}
					}
					set.put(session, session);
					session.put(distanceKey, request.getMasterDistance());
					ConnectResponse.Builder b = ConnectResponse.newBuilder().setIsSuccessful(true);
					MessagePackage.Builder response = MessagePackage.newBuilder().setId(t.getId()).setIsLast(true).setConnectResponse(b);
					sendResponse(session, response.build().toByteArray());
				} finally {
					rwlock.readLock().unlock();
				}
			} else {
				logger.info("this server is not master,close connnection!");
				session.closeSession();
			}

		} else {
			super.fireOnReceive(t, session);
		}
	}

	@Override
	public void onDisConnect(PaxosSession session) {
		Integer masterDistance = session.get(distanceKey);
		Map<PaxosSession, PaxosSession> map = wantConnectMasterSessionMap.get(masterDistance);
		if (map != null) {
			map.remove(session);
		}
	}

	@Override
	public void masterDistanceChange(int distance) {
		try {
			rwlock.writeLock().lock();
			masterDistance = distance;
		} finally {
			rwlock.writeLock().unlock();
		}
		for (Entry<Integer, ConcurrentHashMap<PaxosSession, PaxosSession>> e : wantConnectMasterSessionMap.entrySet()) {
			if (conf.isDebugLog()) {
				logger.debug("this server's master distance change to " + distance);
			}
			if (e.getKey() > masterDistance) {
				for (PaxosSession session : e.getValue().keySet()) {
					session.closeSession();
				}
			}
		}
	}

}
