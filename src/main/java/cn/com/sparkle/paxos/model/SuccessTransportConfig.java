package cn.com.sparkle.paxos.model;

import java.util.List;

public class SuccessTransportConfig {
	private String targetAddress;
	private List<String> targetNotifyAddress;
	private boolean notifyTransValue;

	public SuccessTransportConfig(String targetAddress, List<String> targetNotifyAddress, boolean notifyTransValue) {
		super();
		this.targetAddress = targetAddress;
		this.targetNotifyAddress = targetNotifyAddress;
		this.notifyTransValue = notifyTransValue;
	}

	public String getTargetAddress() {
		return targetAddress;
	}

	public void setTargetAddress(String targetAddress) {
		this.targetAddress = targetAddress;
	}

	public List<String> getTargetNotifyAddress() {
		return targetNotifyAddress;
	}

	public void setTargetNotifyAddress(List<String> targetNotifyAddress) {
		this.targetNotifyAddress = targetNotifyAddress;
	}

	public boolean isNotifyTransValue() {
		return notifyTransValue;
	}

	public void setNotifyTransValue(boolean notifyTransValue) {
		this.notifyTransValue = notifyTransValue;
	}
}
