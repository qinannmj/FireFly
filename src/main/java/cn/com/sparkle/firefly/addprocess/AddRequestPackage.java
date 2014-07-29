package cn.com.sparkle.firefly.addprocess;

import java.util.LinkedList;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.NodesCollection;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.ConfigNodeSet;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.AdminCommand;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.net.netlayer.PaxosSessionKeys;
import cn.com.sparkle.firefly.protocolprocessor.Protocol;
import cn.com.sparkle.firefly.protocolprocessor.negotiation.ServerNegotiationStatus;
import cn.com.sparkle.firefly.state.NodeState;
import cn.com.sparkle.firefly.util.QuorumCalcUtil;

public class AddRequestPackage {
	private LinkedList<AddRequest> valueList = new LinkedList<AddRequest>();
	private PaxosSession session;
	private long valueByteSize = 0;
	private boolean isManageCommand = false;
	private Context context;

	public AddRequestPackage(AddRequest addRequest, PaxosSession session, Context context) {
		super();
		addRequest(addRequest);
		isManageCommand = addRequest.getCommandType().isAdmin();
		this.session = session;
		this.context = context;
	}

	public void addRequest(AddRequest addRequest) {
		if (isManageCommand) {
			throw new RuntimeException("admin package is not permitted to append a request");
		}
		this.valueByteSize += addRequest.getValue().length;
		valueList.addLast(addRequest);
	}

	public void responseAddResponse(long messageId, long instanceId, byte[] bytes) throws UnsupportedChecksumAlgorithm {
		if (session == null) {
			return;
		}
		context.getcState().getSelfState().addResponseCustomAddRequestCount();
		ServerNegotiationStatus negotiationStatus = session.get(PaxosSessionKeys.NEGOTIATION_STATUS_KEY);
		Protocol protocol = negotiationStatus.getProtocol();
		try {
			FrameBody frameBody = new FrameBody(protocol.createAddResponse(messageId, instanceId, bytes, true), session.getChecksumType());
			session.write(frameBody);
		} catch (NetCloseException e) {
		}
	}

	public void responseAdminResponse(long messageId, boolean isSuccess, String error) throws UnsupportedChecksumAlgorithm {
		if (session == null) {
			return;
		}
		context.getcState().getSelfState().addResponseAdminAddRequestCount();
		ServerNegotiationStatus negotiationStatus = session.get(PaxosSessionKeys.NEGOTIATION_STATUS_KEY);
		Protocol protocol = negotiationStatus.getProtocol();
		try {
			FrameBody frameBody = new FrameBody(protocol.createAdminResponse(messageId, isSuccess, error), session.getChecksumType());
			session.write(frameBody);
		} catch (NetCloseException e) {
		}
	}

	public void closeConnection() {
		if (session == null) {
			return;
		}
		session.closeSession();
	}

	public long getValueByteSize() {
		return valueByteSize;
	}

	public PaxosSession getSession() {
		return session;
	}

	public boolean isManageCommand() {
		return isManageCommand;
	}

	public LinkedList<AddRequest> getValueList() {
		return valueList;
	}
	
	public String testAdminToPaxosAble(){
		if(!isManageCommand()){
			return null;
		}else{
			AdminCommand adminCommand = new AdminCommand(valueList.get(0).getValue());
			ConfigNodeSet configNodeSet = context.getConfiguration().getConfigNodeSet();
			int newQuorum = 0;
			if(adminCommand.getType().equals(AdminCommand.ADD_SENATOR)){
				newQuorum = QuorumCalcUtil.calcQuorumNum(configNodeSet.getSenators().size() + 1, context.getConfiguration().getDiskMemLost());
			}else{
				//rm a node
				newQuorum = QuorumCalcUtil.calcQuorumNum(configNodeSet.getSenators().size() - 1, context.getConfiguration().getDiskMemLost());
			}
			String rmAddress = adminCommand.getAddress();
			NodesCollection nodes = context.getcState().getSenators();
			int uptoDateRemainCount = 0;
			for(String ads : nodes.getAllActiveNodes().keySet()){
				if(!ads.equals(rmAddress)){
					NodeState nodeState = nodes.getNodeStates().get(ads);
					if(nodeState.getLastCanExecuteInstanceId() >= configNodeSet.getVersion()){
						++uptoDateRemainCount;
					}
				}
			}
			if(uptoDateRemainCount >= newQuorum){
				return null;
			}else{
				return String.format("error:%s", "Add/Remove senator will leads to inconsistent, pls try again later!");
			}
		}
	}
}