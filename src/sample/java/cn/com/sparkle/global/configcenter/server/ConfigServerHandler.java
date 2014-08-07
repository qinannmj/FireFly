package cn.com.sparkle.global.configcenter.server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.AttributeKey;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Add;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Messages;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Value;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Watch;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ConfigServerHandler extends HandlerInterface {
	private final static Logger logger = Logger.getLogger(ConfigServerHandler.class);

	private final static AttributeKey<HashMap<String, AddRequest>> MAP_KEY = new AttributeKey<HashMap<String, AddRequest>>("map-key");
	private final int SAVE_TO_DISK_THRESHOLD = 50000;// �޸�50000�Σ�dump�ڴ�
	private int curModifyNum = 0;
	private ConcurrentHashMap<String, Value> map; // �ڴ��ֵ��
	private HashMap<String, HashSet<PaxosSession>> watch = new HashMap<String, HashSet<PaxosSession>>(10000);// watch ��ϵ
	public final AtomicLong clientCount = new AtomicLong();
	public final ConcurrentHashMap<PaxosSession,PaxosSession> clientset = new ConcurrentHashMap<PaxosSession,PaxosSession>();

	private String path;// �ļ�·��
	private ReentrantLock lock = new ReentrantLock();

	public ConfigServerHandler(ConcurrentHashMap<String, Value> map, String path) {
		this.map = map;
		this.path = path;
	}

	@Override
	public void onClientConnect(PaxosSession session) {
		try{
		logger.debug(String.format("user connected remote %s to local %s", session.getRemoteAddress(),session.getLocalAddress()));
		}catch(Exception e){
			e.printStackTrace();
		}
		clientCount.incrementAndGet();
		clientset.put(session, session);
	}

	@Override
	public void onClientClose(PaxosSession session) {
		clientCount.decrementAndGet();
		clientset.remove(session);
		// ��� session ע�������watch

		HashMap<String, AddRequest> sessionWatch = session.get(MAP_KEY);
		if (sessionWatch != null) {
			for (String key : sessionWatch.keySet()) {
				HashSet<PaxosSession> s = watch.get(key);
				if (s != null) {
					synchronized (s) {
						logger.debug("clear a session");
						s.remove(session);
					}
				}

			}
		}
	}

	@Override
	public void onReceiveLookUp(PaxosSession session, AddRequest addRequest) {
		try {
			Messages m = Messages.parseFrom(addRequest.getValue());
			if (m.hasWatch()) {
				Watch w = m.getWatch();
				// ��¼��ǰsession,watch����Щ��¼
				HashMap<String, AddRequest> sessionWatch = session.get(MAP_KEY);
				if (sessionWatch == null) {
					sessionWatch = new HashMap<String, AddRequest>();
					session.put(MAP_KEY,sessionWatch);
				}
				sessionWatch.put(w.getKey(), addRequest);
				// ��¼��Щ��¼��session,watch��
				HashSet<PaxosSession> s = watch.get(w.getKey());
				if (s == null) {
					try {
						lock.lock();
						if (s == null) {
							s = new HashSet<PaxosSession>();
							watch.put(w.getKey(), s);
						}
					} finally {
						lock.unlock();
					}
				}
				// ���ص�һ��watch���
				byte[] result = null;
				synchronized (s) {
					Value v = map.get(w.getKey());
					if (v == null || v.getVersion() <= w.getVersion()) {
						result = Value.newBuilder().setValue(ByteString.EMPTY).setVersion(-1).build().toByteArray();

					} else {
						result = v.toByteArray();

					}
					s.add(session);
				}
				logger.debug("watch   packageID:" + addRequest.getMessageId());
				this.sendNotifyMessageResponse(session, addRequest, result);
			}
		} catch (InvalidProtocolBufferException e1) {
			logger.error("fatal error", e1);
			session.closeSession();
			return;
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("fatal error", e);
			session.closeSession();
			return;
		}

	}

	public byte[] onLoged(byte[] bytes,int offset, int length) {
		// д��paxos cluster �ɹ�
		try {
			Add add = Add.parseFrom(ByteString.copyFrom(bytes, offset, length));
			Value v = map.get(add.getKey());
			// �޸��ڴ�
			if (v == null) {
				v = Value.newBuilder().setValue(add.getValue()).setVersion(0).build();
				map.put(add.getKey(), v);
			} else {
				v = Value.newBuilder().setValue(add.getValue()).setVersion(v.getVersion() + 1).build();
				map.put(add.getKey(), v);
			}
			// ��¼��Ҫ֪ͨ��client
			HashSet<PaxosSession> s = null;
			try {
				lock.lock();
				s = watch.get(add.getKey());
			} finally {
				lock.unlock();
			}
			if (s != null) {
				LinkedList<PaxosSession> list = new LinkedList<PaxosSession>();
				synchronized (s) {
					list.addAll(s); // ��¼��Ҫ֪ͨ��session���ͷ���
				}
				logger.debug("notify client modification! client num is " + list.size());
				for (PaxosSession session : list) {
					HashMap<String, AddRequest> sessionWatch = session.get(MAP_KEY);
					AddRequest addRequest = sessionWatch.get(add.getKey());
					this.sendNotifyMessageResponse(session, addRequest, v.toByteString().toByteArray());// ֪ͨ����watch����,���ڷ�����nio��ܣ�ע����Ϣ����
				}
			}
			++curModifyNum;

			return new byte[] { 1 };
		} catch (Throwable e) {
			logger.error("fatal error", e);
		}
		return new byte[] { 0 };
	}

	@Override
	public void onInstanceIdExecuted(long instanceId) {
		if (curModifyNum >= SAVE_TO_DISK_THRESHOLD) {

			// ���浱ǰ�ڴ�dump��Ӳ��
			try {
				Repository.saveToDisk(this.map, instanceId, path);
			} catch (NoSuchAlgorithmException e) {
				logger.error("fatal error", e);
			} catch (IOException e) {
				logger.error("fatal error", e);
				System.exit(1);
			}
			curModifyNum = 0;
		}
	}

}
