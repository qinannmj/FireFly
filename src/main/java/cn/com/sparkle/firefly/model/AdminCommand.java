package cn.com.sparkle.firefly.model;

public class AdminCommand {
	public final static String ADD_SENATOR = "add";
	public final static String REMOVE_SENATOR = "rm";
	
	private String address;
	private String type;

	public AdminCommand(String address, String type) {
		super();
		this.address = address;
		this.type = type;
	}

	public AdminCommand(byte[] bytes) {
		String[] s = new String(bytes).split(" ");
		type = s[0];
		address = s[1];
	}

	public String getAddress() {
		return address;
	}

	public String getType() {
		return type;
	}

	public byte[] toBytes() {
		StringBuffer sb = new StringBuffer();
		sb.append(type).append(" ").append(address);
		return sb.toString().getBytes();
	}

}
