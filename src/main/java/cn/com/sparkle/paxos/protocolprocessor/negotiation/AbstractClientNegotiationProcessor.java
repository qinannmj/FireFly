package cn.com.sparkle.paxos.protocolprocessor.negotiation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Version;
import cn.com.sparkle.paxos.checksum.ChecksumUtil;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.net.client.NetNode;
import cn.com.sparkle.paxos.net.frame.FrameBody;
import cn.com.sparkle.paxos.net.netlayer.NetCloseException;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.paxos.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolManager;

public abstract class AbstractClientNegotiationProcessor extends AbstractChainProtocolProcessor<FrameBody> implements ClientNegotiationProcessor {

	private Logger logger = Logger.getLogger(AbstractClientNegotiationProcessor.class);

	private int preferChecksumType;

	private ProtocolManager protocolManager;

	private int heartBeatIntervale;

	public AbstractClientNegotiationProcessor(int preferChecksumType, int heartBeatInterval, ProtocolManager protocolManager) {
		this.preferChecksumType = preferChecksumType;
		this.protocolManager = protocolManager;
		this.heartBeatIntervale = heartBeatInterval;
	}

	@Override
	public void negotiation(PaxosSession session, String nodeAddress , String targetAddress) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
		pw.println(nodeAddress);
		pw.println(targetAddress);
		pw.println(Version.APP_VER);
		for (String protocol : this.protocolManager.getCompatibleProtocolVersion()) {
			pw.print(protocol + ",");
		}
		pw.println();

		pw.print(preferChecksumType + ","); // add a checksumtype that is prefer to use.
		for (String checksum : ChecksumUtil.COMPATIBALE_CHECKSUM_TYPE) {
			if (!String.valueOf(preferChecksumType).equals(checksum)) {
				pw.print(checksum + ",");
			}
		}
		pw.println();
		pw.flush();

		try {
			FrameBody body = new FrameBody(baos.toByteArray(), ChecksumUtil.PURE_JAVA_CRC32);
			session.write(body);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("unreachable exception", e);
		} catch (NetCloseException e) {
		}
	}

	@Override
	public void receive(FrameBody body, PaxosSession session) throws InterruptedException {
		NetNode netNode = session.get(PaxosSessionKeys.NET_NODE_KEY);

		if (netNode != null) {
			netNode.getProtocol().getClientReceiveProcessor().receive(body, session);
			super.fireOnReceive(body, session);
			return;
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(body.getBody());
		BufferedReader br = new BufferedReader(new InputStreamReader(bais));
		String address = session.get(PaxosSessionKeys.ADDRESS_KEY);
		if(address == null){
			throw new RuntimeException("address is null");
		}
		try {
			String appVersion = br.readLine();
			String protocolVersion = br.readLine();
			String checksumType = br.readLine();
			int _heartBeatInterval = Integer.parseInt(br.readLine());
			@SuppressWarnings("unused")
			String errorCode = br.readLine();//pre remain
			
			if (!protocolVersion.equals("") && !checksumType.equals("")) {
				Protocol protocol = protocolManager.getProtocol(protocolVersion);
				session.setChecksumType(Integer.parseInt(checksumType));
				netNode = createNetNode(appVersion, address, session, protocol, Math.min(_heartBeatInterval, heartBeatIntervale));

				session.put(PaxosSessionKeys.NET_NODE_KEY, netNode);
			} else {
				logger.error("uncompatible protocol to remoting server that appversion is " + appVersion);
				session.closeSession();
			}

		} catch (Throwable e) {
			logger.error("error negotiation response", e);
			session.closeSession();
		}
	}

	public ProtocolManager getProtocolManager() {
		return protocolManager;
	}

	public abstract NetNode createNetNode(String appVersion, String address, PaxosSession session, Protocol protocol, int heartBeatInterval);
}
