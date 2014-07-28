package cn.com.sparkle.firefly.stablestorage.model;

public enum RecordType {
	SUCCESS(0x80), VOTE(0x00);
	private int value;

	private RecordType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
