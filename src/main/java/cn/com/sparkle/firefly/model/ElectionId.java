package cn.com.sparkle.firefly.model;

public class ElectionId extends Id {
	private long version;

	public ElectionId(String address, long increaseId, long version) {
		super(address, increaseId);
		this.version = version;
	}

	public long getVersion() {
		return version;
	}
	@Override
	public int compareTo(Id o) {
		ElectionId id = (ElectionId)o;
		int result;
		if (version == id.getVersion()) {
			return super.compareTo(o);
		} else {
			result = version > id.getVersion() ? 1 : -1;
		}

		return result;
	}
	@Override
	public String toString() {
		return super.toString() + " " + version;
	}
}
