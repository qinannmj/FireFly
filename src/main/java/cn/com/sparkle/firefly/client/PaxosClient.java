package cn.com.sparkle.firefly.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.client.deamon.MasterHeartBeatDeamon;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;

public class PaxosClient {
	private final static Logger logger = Logger.getLogger(PaxosClient.class);

	public static interface CommandCallBack {
		public void response(byte[] result, long instanceId);
	}

	//	private String[] senators;
	private CommandAsyncProcessor[] processor;
	private AtomicInteger flag = new AtomicInteger(0);
	private AtomicLong responseInstanceId = new AtomicLong(-1);
	@SuppressWarnings("unused")

	/**
	 * @param senator
	 *            some address of cluster
	 * @param netLayerConfPath
	 *            configuration of net layer
	 * @param netLayerType
	 *            netty or raptor
	 * @param preferChecksumType    ChecksumFactory.
	 * @param tcpConnectNum
	 * @param isPromiseMaster
	 *            true indicates the server that is not master will disconnect
	 *            the connection
	 * @throws Throwable
	 */
	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, int singleTcpWaitingMaxMem) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, ProtocolManager
				.createClientProtocolManager(), singleTcpWaitingMaxMem);
	}

	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, ProtocolManager
				.createClientProtocolManager());
	}


	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, ProtocolManager protocolManager) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, protocolManager, 524288);
	}

	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, ProtocolManager protocolManager, int singleTcpWaitingMaxMem) throws Throwable {
		masterDistance = Math.min(Constants.MAX_MASTER_DISTANCE, masterDistance);
		if (tcpConnectNum < 1) {
			tcpConnectNum = 1;
		}
		CommandAsyncProcessor.init(netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, protocolManager);
		processor = new CommandAsyncProcessor[tcpConnectNum];
		MasterHeartBeatDeamon heartBeatDeamon = new MasterHeartBeatDeamon();
		heartBeatDeamon.setName("heartBeatDeamon");
		for (int i = 0; i < tcpConnectNum; ++i) {
			processor[i] = new CommandAsyncProcessor(senator, masterDistance, singleTcpWaitingMaxMem / 3, singleTcpWaitingMaxMem * 2 / 3, heartBeatDeamon);
			processor[i].runDeamon();
		}

		heartBeatDeamon.start();
	}

	public PaxosOperater getOperator() {
		int nextFlag;
		for (;;) {
			int curFlag = flag.get();
			if (curFlag == Integer.MAX_VALUE) {
				if (flag.compareAndSet(curFlag, 0)) {
					nextFlag = 0;
					break;
				}
			} else {
				nextFlag = curFlag + 1;
				if (flag.compareAndSet(curFlag, nextFlag)) {
					break;
				}
			}
		}
		return new DefaultPaxosOperator(processor[nextFlag % processor.length], this.responseInstanceId);
	}

	public void changeSenator(String[] senator) throws Throwable {
		StringBuffer sb = new StringBuffer();
		for (String s : senator) {
			sb.append(s).append(" ");
		}
		logger.info("senators change to " + sb.toString());
		for (CommandAsyncProcessor p : processor) {
			p.changeSenator(senator);
		}
	}

	public long getResponseInstanceId() {
		return this.responseInstanceId.get();
	}
}
