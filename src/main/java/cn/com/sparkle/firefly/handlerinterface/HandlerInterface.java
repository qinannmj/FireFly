package cn.com.sparkle.firefly.handlerinterface;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.addprocess.AddRequestDealer;
import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.Value.IterElement;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.net.userserver.NotPermissionLogOnMasterExeception;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.ServerNegotiationStatus;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;

public abstract class HandlerInterface {
	private final static Logger logger = Logger.getLogger(HandlerInterface.class);
	private AddRequestDealer addRequestDealer;
	private AccountBook aBook;

	public abstract void onClientConnect(PaxosSession session);

	public abstract void onClientClose(PaxosSession session);

	public abstract void onReceiveLookUp(PaxosSession session, AddRequest request);

	public abstract byte[] onLoged(byte[] bytes,int offset,int length);
	
	public abstract void onInstanceIdExecuted(long instanceId);

	public final void onCommandReceive(PaxosSession session, AddRequest request) {
		if (request.getCommandType().isWrite()) {
			try {
				log(session, request);
			} catch (NotPermissionLogOnMasterExeception e) {
				logger.error("fatal error", e);
			}
		} else {
			onReceiveLookUp(session, request);
		}
	}
	
	public final void setAccountBook(AccountBook aBook){
		this.aBook = aBook;
	}
	
	/**
	 * 
	 * @param successfulRecordWrap
	 * @param instanceId
	 * @param v
	 * @param aBook
	 * @return command num
	 * @throws IOException
	 */
	public final int onLoged(SuccessfulRecordWrap successfulRecordWrap, long instanceId, Value v, AccountBook aBook) throws IOException {
		LinkedList<byte[]>  customResult = new LinkedList<byte[]>();
		Iterator<IterElement> iter = v.iterator();
		int commCount = 0;
		while(iter.hasNext()){
			IterElement ie = iter.next();
			customResult.add(onLoged(v.getValuebytes(),ie.getOffset(),ie.getSize()));
			++commCount;
		}
		onInstanceIdExecuted(instanceId);
		finishLogedProcess(successfulRecordWrap, customResult);
		return commCount;
	}

	public final void setAddRequestDealer(AddRequestDealer addRequestDealer) {
		this.addRequestDealer = addRequestDealer;
	}

	public final void log(PaxosSession session, AddRequest request) throws NotPermissionLogOnMasterExeception {
		if (request.getCommandType().isWrite()) {
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
			sendMessage(session, request.getMessageId(), -1, response, true);
			
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

	public final void sendNotifyMessageResponse(PaxosSession session, AddRequest addRequest, byte[] notifyMessage)
			throws UnsupportedChecksumAlgorithm {
		sendMessage(session, addRequest.getMessageId(), -1, notifyMessage, false);
	}

	private void sendMessage(PaxosSession session, long messageId, long instanceId, byte[] bytes, boolean isLast) throws UnsupportedChecksumAlgorithm {
		ServerNegotiationStatus negotiationStatus = session.get(PaxosSessionKeys.NEGOTIATION_STATUS_KEY);
		Protocol protocol = negotiationStatus.getProtocol();
		try {
			FrameBody frameBody = new FrameBody(protocol.createAddResponse(messageId, instanceId, bytes, isLast), session.getChecksumType());
			session.write(frameBody);
		} catch (NetCloseException e) {
		}
	}

	public final void finishLogedProcess(SuccessfulRecordWrap recordWrap, List<byte[]> customResult) throws IOException {
		if (recordWrap.getAddRequestPackages() != null) {
			// send add response
			LinkedList<AddRequestPackage> addRequestPackages = recordWrap.getAddRequestPackages();
			Iterator<byte[]> iter = customResult.iterator();
			for (AddRequestPackage arp : addRequestPackages) {
				for (AddRequest addRequest : arp.getValueList()) {
					try {
						arp.responseAddResponse(addRequest.getMessageId(), recordWrap.getInstanceId(), iter.next());
					} catch (UnsupportedChecksumAlgorithm e) {
						logger.error("unexcepted error", e);
					}
				}
				
			}
		}
	}

	public final void writeExecuteLog(long instanceId) throws IOException, UnsupportedChecksumAlgorithm {
		aBook.finishCurInstance(instanceId);
	}
}
