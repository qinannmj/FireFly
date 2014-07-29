package cn.com.sparkle.firefly.protocolprocessor.negotiation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Version;
import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.AbstractChainProtocolProcessor;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolProcessorChain;

/**
 * 
 * ------------------------------------------------------------
 * request
 * nodeAddress /r
 * targetAddress/r
 * appversion /r
 * protocol version /r
 * checksum type /r
 * customdate
 * 
 * response
 * appversion /r
 * accepted protocol version /r
 * accepted checksum type /r
 * self heart beat interval /r
 * errorcode /r   pre remained
 * customdate
 * ------------------------------------------------------------
 * @author qinan.qn
 *
 */

public abstract class AbstractServerProtocolNegotiationProcessor extends AbstractChainProtocolProcessor<FrameBody> {

	private final static Logger logger = Logger.getLogger(AbstractServerProtocolNegotiationProcessor.class);

	private ProtocolManager protocolManager;

	private Configuration conf;

	public AbstractServerProtocolNegotiationProcessor(ProtocolManager protocolManager, Configuration conf) {
		this.protocolManager = protocolManager;
		this.conf = conf;
	}

	@Override
	public void receive(FrameBody body, PaxosSession session) throws InterruptedException {
		ServerNegotiationStatus negotiationStatus = session.get(PaxosSessionKeys.NEGOTIATION_STATUS_KEY);
		if (negotiationStatus != null) {
			negotiationStatus.getChain().receive(body, session); // go on to process
			return;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body.getBody())));

		try {
			String nodeAddress = br.readLine();
			String targetAddress = br.readLine();
			String appVersion = br.readLine();
			String[] protocolVersion = br.readLine().split(",");
			String[] checksumType = br.readLine().split(",");

			String acceptProtocolVersion = "";
			String acceptChecksumType = "";

			if (isAcceptConnect(targetAddress, nodeAddress)) {

				Set<String> compatibleProtocolVersion = this.protocolManager.getCompatibleProtocolVersion();
				for (String pv : protocolVersion) {
					if (compatibleProtocolVersion.contains(pv)) {
						acceptProtocolVersion = pv;
						break;
					}
				}

				for (String ct : checksumType) {
					if (ChecksumUtil.COMPATIBALE_CHECKSUM_TYPE.contains(ct)) {
						acceptChecksumType = ct;
						break;
					}
				}

				if (!acceptChecksumType.equals("") && !acceptProtocolVersion.equals("")) {
					//accept negotiation result
					ProtocolProcessorChain chain = getChain(acceptProtocolVersion);
					ServerNegotiationStatus status = new ServerNegotiationStatus(appVersion, chain, protocolManager.getProtocol(acceptProtocolVersion));
					session.setChecksumType(Integer.parseInt(acceptChecksumType));
					session.put(PaxosSessionKeys.NEGOTIATION_STATUS_KEY, status);
				}
			}
			// send the negotiation result
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
			pw.println(Version.APP_VER);
			pw.println(acceptProtocolVersion);
			pw.println(acceptChecksumType);
			pw.println(conf.getHeartBeatInterval());
			pw.println("");
			writeCustomParam(pw);
			pw.flush();
			try {
				FrameBody frameBody = new FrameBody(baos.toByteArray(), ChecksumUtil.PURE_JAVA_CRC32);
				try {
					session.write(frameBody);
				} catch (NetCloseException e) {
					//client has been closed, this situation don't have to process. 
				}
			} catch (UnsupportedChecksumAlgorithm e) {
				logger.error("unexception erorr", e);
			}
			session.put(PaxosSessionKeys.ADDRESS_KEY, nodeAddress);
		} catch (IOException e) {
			logger.error("unexception error, may be this is attack, close the connection");
			session.closeSession();
		}
	}

	public ProtocolManager getProtocolManager() {
		return protocolManager;
	}

	protected abstract void writeCustomParam(PrintWriter pw);
	
	protected abstract ProtocolProcessorChain getChain(String version);

	protected abstract boolean isAcceptConnect(String targetAddress, String sourceAddress);
}
