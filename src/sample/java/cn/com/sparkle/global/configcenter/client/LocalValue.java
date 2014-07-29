package cn.com.sparkle.global.configcenter.client;

public class LocalValue {
	private String value;
	private long version;
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public LocalValue(String value, long version) {
		super();
		this.value = value;
		this.version = version;
	}
}
