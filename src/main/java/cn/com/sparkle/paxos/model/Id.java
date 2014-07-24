package cn.com.sparkle.paxos.model;

public class Id implements Comparable<Id> {
	private String address;
	private long increaseId;

	public Id(String address, long increaseId) {
		super();
		this.address = address;
		this.increaseId = increaseId;
	}

	@Override
	public int compareTo(Id o) {
		int result;
		if (getIncreaseId() == o.getIncreaseId()) {
			result = getAddress().compareTo(o.getAddress());
			if (result != 0) {
				result = result > 0 ? 1 : -1;
			}
		} else {
			result = getIncreaseId() > o.getIncreaseId() ? 1 : -1;
		}

		return result;
	}

	public String getAddress() {
		return address;
	}

	public long getIncreaseId() {
		return increaseId;
	}

	public void setIncreaseId(long increaseId) {
		this.increaseId = increaseId;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	@Override
	public String toString() {
		return increaseId + " " + address;
	}
}
