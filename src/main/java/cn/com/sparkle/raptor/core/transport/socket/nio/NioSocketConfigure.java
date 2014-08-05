package cn.com.sparkle.raptor.core.transport.socket.nio;

import java.net.Socket;
import java.net.SocketException;

public class NioSocketConfigure {
	private int recieveBuffSize = 8 * 1024;
	private int sentBuffSize = 8 * 1024;
	private boolean tcpNoDelay = false;
	private boolean keepAlive = true;
	private boolean oobInline;
	private Boolean reuseAddress = true;
	private int soLinger;
	private int trafficClass;
	private int processorNum = 1;
	private int registerWriteDelay = 0;
	private int registerReadDelay = 0;
	private int reRegisterWriteDelay = 100;
	private int registerConnecterDelay = 0;
	private int clearTimeoutSessionInterval = 300000;
	private int trySendNum = 60;
	private boolean isDaemon = false;
	private int cycleRecieveBuffSize = 8 * 1024;
	private int cycleRecieveBuffCellSize = 20000;
	private int soTimeOut = 500;
	private int backLog = 500;

	public int getCycleRecieveBuffCellSize() {
		return cycleRecieveBuffCellSize;
	}

	public int getSoTimeOut() {
		return soTimeOut;
	}

	public void setSoTimeOut(int soTimeOut) {
		this.soTimeOut = soTimeOut;
	}

	public void setCycleRecieveBuffCellSize(int cycleRecieveBuffCellSize) {
		this.cycleRecieveBuffCellSize = cycleRecieveBuffCellSize;
	}

	public int getCycleRecieveBuffSize() {
		return cycleRecieveBuffSize;
	}

	public void setCycleRecieveBuffSize(int cycleRecieveBuffSize) {
		this.cycleRecieveBuffSize = cycleRecieveBuffSize;
	}

	public void setProcessorNum(int processorNum) {
		this.processorNum = processorNum;
	}

	public int getBackLog() {
		return backLog;
	}

	public void setBackLog(int backLog) {
		this.backLog = backLog;
	}

	public int getTrySendNum() {
		return trySendNum;
	}

	public void setTrySendNum(int trySendNum) {
		this.trySendNum = trySendNum;
	}

	public int getClearTimeoutSessionInterval() {
		return clearTimeoutSessionInterval;
	}

	public void setClearTimeoutSessionInterval(int clearTimeoutSessionInterval) {
		this.clearTimeoutSessionInterval = clearTimeoutSessionInterval;
	}

	public int getRegisterConnecterDelay() {
		return registerConnecterDelay;
	}

	public void setRegisterConnecterDelay(int registerConnecterDelay) {
		this.registerConnecterDelay = registerConnecterDelay;
	}

	public int getRegisterWriteDelay() {
		return registerWriteDelay;
	}

	public void setRegisterWriteDelay(int registerWriteDelay) {
		this.registerWriteDelay = registerWriteDelay;
	}

	public int getRegisterReadDelay() {
		return registerReadDelay;
	}

	public void setRegisterReadDelay(int registerReadDelay) {
		this.registerReadDelay = registerReadDelay;
	}

	public int getReRegisterWriteDelay() {
		return reRegisterWriteDelay;
	}

	public void setReRegisterWriteDelay(int reRegisterWriteDelay) {
		this.reRegisterWriteDelay = reRegisterWriteDelay;
	}

	public boolean isDaemon() {
		return isDaemon;
	}

	public void setDaemon(boolean isDaemon) {
		this.isDaemon = isDaemon;
	}

	public Integer getProcessorNum() {
		return processorNum;
	}

	public void setProcessorNum(Integer processorNum) {
		this.processorNum = processorNum;
	}

	public void setSentBuffSize(Integer sentBuffSize) {
		this.sentBuffSize = sentBuffSize;
	}

	public void setTcpNoDelay(Boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public void setKeepAlive(Boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public void setOobInline(Boolean oobInline) {
		this.oobInline = oobInline;
	}

	public void setReuseAddress(Boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

	public void setSoLinger(Integer soLinger) {
		this.soLinger = soLinger;
	}

	public void setTrafficClass(Integer trafficClass) {
		this.trafficClass = trafficClass;
	}

	public int getRecieveBuffSize() {
		return recieveBuffSize;
	}

	public void setRecieveBuffSize(int recieveBuffSize) {
		this.recieveBuffSize = recieveBuffSize;
	}

	public Integer getSentBuffSize() {
		return sentBuffSize;
	}

	public void setSentBuffSize(int sentBuffSize) {
		this.sentBuffSize = Integer.valueOf(sentBuffSize);
	}

	public Boolean getTcpNoDelay() {
		return tcpNoDelay;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = Boolean.valueOf(tcpNoDelay);
	}

	public Boolean getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = Boolean.valueOf(keepAlive);
	}

	public Boolean getOobInline() {
		return oobInline;
	}

	public void setOobInline(boolean oobInline) {
		this.oobInline = Boolean.valueOf(oobInline);
	}

	public Boolean getReuseAddress() {
		return reuseAddress;
	}

	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = Boolean.valueOf(reuseAddress);
	}

	public Integer getSoLinger() {
		return soLinger;
	}

	public void setSoLinger(int soLinger) {
		this.soLinger = Integer.valueOf(soLinger);
	}

	public Integer getTrafficClass() {
		return trafficClass;
	}

	public void setTrafficClass(int trafficClass) {
		this.trafficClass = Integer.valueOf(trafficClass);
	}

	public void configurateSocket(Socket socket) throws SocketException {
		if (getKeepAlive() != null)
			socket.setKeepAlive(getKeepAlive().booleanValue());
		if (getOobInline() != null)
			socket.setOOBInline(getOobInline().booleanValue());
		if (getReuseAddress() != null)
			socket.setReuseAddress(getReuseAddress().booleanValue());
		if (getRecieveBuffSize() > socket.getReceiveBufferSize())
			socket.setReceiveBufferSize(getRecieveBuffSize());

		if (getSentBuffSize() != null && getSentBuffSize().intValue() > socket.getSendBufferSize())
			socket.setSendBufferSize(getSentBuffSize().intValue());
		if (getSoLinger() != null)
			if (getTcpNoDelay() != null)
				socket.setTcpNoDelay(getTcpNoDelay().booleanValue());
		if (getTrafficClass() != null)
			socket.setTrafficClass(getTrafficClass().intValue());
		socket.setSoTimeout(getSoTimeOut());
	}
}
