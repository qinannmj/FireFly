package cn.com.sparkle.firefly.client;

import java.util.concurrent.atomic.AtomicInteger;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.client.deamon.MasterHeartBeatDeamon;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;

public class PaxosClient {

	public static interface CommandCallBack {
		public void response(byte[] result,long instanceId);
	}

	//	private String[] senators;
	private CommandAsyncProcessor[] processor;
	private AtomicInteger flag = new AtomicInteger(0);
	@SuppressWarnings("unused")
	private boolean debugLog;

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
			int masterDistance) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, ProtocolManager
				.createClientProtocolManager(), false);
	}

	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, ProtocolManager protocolManager) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, protocolManager, false);
	}

	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, boolean debugLog) throws Throwable {
		this(senator, netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, tcpConnectNum, masterDistance, ProtocolManager
				.createClientProtocolManager(), debugLog);
	}

	public PaxosClient(String[] senator, String netLayerConfPath, String netLayerType, int preferChecksumType, int heartBeatInterval, int tcpConnectNum,
			int masterDistance, ProtocolManager protocolManager, boolean debugLog) throws Throwable {
		masterDistance = Math.min(Constants.MAX_MASTER_INSTANCE, masterDistance);
		this.debugLog = debugLog;
		if (tcpConnectNum < 1) {
			tcpConnectNum = 1;
		}
		CommandAsyncProcessor.init(netLayerConfPath, netLayerType, preferChecksumType, heartBeatInterval, protocolManager, debugLog);
		processor = new CommandAsyncProcessor[tcpConnectNum];
		MasterHeartBeatDeamon heartBeatDeamon = new MasterHeartBeatDeamon(debugLog);
		heartBeatDeamon.setName("heartBeatDeamon");
		for (int i = 0; i < tcpConnectNum; ++i) {
			processor[i] = new CommandAsyncProcessor(senator, 1000, 100, masterDistance, heartBeatDeamon, debugLog);
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
		return new PaxosOperater(processor[nextFlag % processor.length]);
	}

	public void changeSenator(String[] senator) throws Throwable {
		for (CommandAsyncProcessor p : processor) {
			p.changeSenator(senator);
		}
	}
}
