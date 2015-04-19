package cn.com.sparkle.firefly.state;

import java.util.LinkedList;
import java.util.List;

import cn.com.sparkle.firefly.model.ElectionId;

public class NodeState {
	private String address;
	private long lastBeatHeatTime = 0;
	private Long lastCanExecuteInstanceId = -1l;
	private ElectionId lastElectionId = new ElectionId("", -1, -1);

	private boolean isMasterConnected = true;

	private boolean isInit = false;

	private boolean isConnected = false;;

	private boolean isUpToDate = false;
	
	private String room = "";
	
	private boolean isArbitrator;

	private int masterDistance = Integer.MAX_VALUE;

	private List<String> connectedValidNode = new LinkedList<String>();
	
	public NodeState(String address) {
		this.address = address;
	}
	
	public boolean isUpToDate() {
		return isUpToDate;
	}

	public int getMasterDistance() {
		return masterDistance;
	}

	public void setMasterDistance(int masterDistance) {
		if(masterDistance == 2147483647){
			masterDistance = 2147483647;
		}
		this.masterDistance = masterDistance;
	}

	public List<String> getConnectedValidNode() {
		return connectedValidNode;
	}

	public void setConnectedValidNode(List<String> connectedValidNode) {
		this.connectedValidNode = connectedValidNode;
	}

	public void setUpToDate(boolean isUpToDate) {
		this.isUpToDate = isUpToDate;
	}

	

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public long getLastBeatHeatTime() {
		return lastBeatHeatTime;
	}

	public void setLastBeatHeatTime(long lastBeatHeatTime) {
		this.lastBeatHeatTime = lastBeatHeatTime;
	}

	public Long getLastCanExecuteInstanceId() {
		return lastCanExecuteInstanceId;
	}

	public void setLastCanExecuteInstanceId(Long lastCanExecuteInstanceId) {
		this.lastCanExecuteInstanceId = lastCanExecuteInstanceId;
	}

	public ElectionId getLastElectionId() {
		return lastElectionId;
	}

	public void setLastElectionId(ElectionId lastElectionId) {
		this.lastElectionId = lastElectionId;
	}

	public boolean isMasterConnected() {
		return isMasterConnected;
	}

	public void setMasterConnected(boolean isMasterConnected) {
		this.isMasterConnected = isMasterConnected;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public boolean isInit() {
		return isInit;
	}

	public void setInit(boolean isInit) {
		this.isInit = isInit;
	}

	public boolean isArbitrator() {
		return isArbitrator;
	}

	public void setArbitrator(boolean isArbitrator) {
		this.isArbitrator = isArbitrator;
	}
	

}
