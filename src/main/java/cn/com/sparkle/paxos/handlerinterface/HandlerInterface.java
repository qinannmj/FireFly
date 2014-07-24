package cn.com.sparkle.paxos.handlerinterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.addprocess.AddRequestDealer;
import cn.com.sparkle.paxos.addprocess.AddRequestPackage;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.net.frame.FrameBody;
import cn.com.sparkle.paxos.net.netlayer.NetCloseException;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;
import cn.com.sparkle.paxos.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.paxos.net.userserver.NotPermissionLogOnMasterExeception;
import cn.com.sparkle.paxos.protocolprocessor.Protocol;
import cn.com.sparkle.paxos.protocolprocessor.negotiation.ServerNegotiationStatus;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.stablestorage.model.SuccessfulRecordWrap;

public abstract class HandlerInterface {
	private final static Logger logger = Logger.getLogger(HandlerInterface.class);
	private AddRequestDealer addRequestDealer;

	public abstract void onClientConnect(PaxosSession session);

	public abstract void onClientClose(PaxosSession session);

	public abstract void onReceiveLookUp(PaxosSession session, AddRequest request);

	public abstract byte[] onLoged(byte[] bytes);

	public final void onCommandReceive(PaxosSession session, AddRequest request) {
		if (request.isWrite()) {
			try {
				log(session, request);
			} catch (NotPermissionLogOnMasterExeception e) {
				logger.error("fatal error", e);
			}
		} else {
			onReceiveLookUp(session, request);
		}
	}

	public final void onLoged(SuccessfulRecordWrap successfulRecordWrap, long instanceId, List<byte[]> customCommand, AccountBook aBook) throws IOException {
		ArrayList<byte[]> customResult = new ArrayList<byte[]>(customCommand.size());
		for (byte[] bs : customCommand) {
			customResult.add(onLoged(bs));
		}
		finishLogedProcess(successfulRecordWrap, customResult);
		try {
			writeExecuteLog(instanceId, aBook);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("unexcepted error", e);
		}
	}

	public final void setAddRequestDealer(AddRequestDealer addRequestDealer) {
		this.addRequestDealer = addRequestDealer;
	}

	public final void log(PaxosSession session, AddRequest request) throws NotPermissionLogOnMasterExeception {
		if (request.isWrite()) {
			addRequestDealer.add(session, request);
		} else {
			throw new NotPermissionLogOnMasterExeception("this commad can't be log because it is not write command!");
		}
	}

	/**
	 * this will finish command ,and clear callback function in client
	 * 
	 * @param session
	 * @param addRequest
	 * @param response
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public final void sendResponseCommandResponse(PaxosSession session, AddRequest request, byte[] response) {
		try {
			sendMessage(session, request.getMessageId(), response, true);
		} catch (UnsupportedChecksumAlgorithm e) {
			session.closeSession();
		}
	}

	/**
	 * this will can send data to client continously ,and unclear callback
	 * function in client
	 * 
	 * @param session
	 * @param addRequest
	 * @param response
	 * @throws UnsupportedChecksumAlgorithm 
	 */

	public final void sendNotifyMessageResponse(PaxosSession session, AddRequest addRequest, byte[] notifyMessage) throws UnsupportedChecksumAlgorithm {
		sendMessage(session, addRequest.getMessageId(), notifyMessage, false);
	}

	private void sendMessage(PaxosSession session, long messageId, byte[] bytes, boolean isLast) throws UnsupportedChecksumAlgorithm {
		ServerNegotiationStatus negotiationStatus = session.get(PaxosSessionKeys.NEGOTIATION_STATUS_KEY);
		Protocol protocol = negotiationStatus.getProtocol();
		try {
			FrameBody frameBody = new FrameBody(protocol.createAddResponse(messageId, bytes, isLast), session.getChecksumType());
			session.write(frameBody);
		} catch (NetCloseException e) {
		}
	}

	public final void finishLogedProcess(SuccessfulRecordWrap recordWrap, ArrayList<byte[]> customResult) throws IOException {
		if (recordWrap.getAddRequestPackages() != null) {
			// send add response
			LinkedList<AddRequestPackage> addRequestPackages = recordWrap.getAddRequestPackages();
			int resultIndex = 0;
			for (AddRequestPackage arp : addRequestPackages) {
				for (AddRequest addRequest : arp.getValueList()) {
					try {
						arp.responseAddResponse(addRequest.getMessageId(), customResult.get(resultIndex++));
					} catch (UnsupportedChecksumAlgorithm e) {
						logger.error("unexcepted error", e);
					}
				}
			}
		}
	}

	public final void writeExecuteLog(long instanceId, AccountBook aBook) throws IOException, UnsupportedChecksumAlgorithm {
		try {
			aBook.finishCurInstance(instanceId);
		} catch (IOException e) {
			logger.error("fatal error", e);
			throw e;
		}
	}
}
