package cn.com.sparkle.firefly.model;

public class AdminCommand {
	public final static String ADD_SENATOR = "add";
	public final static String REMOVE_SENATOR = "rm";
	
	private String address;
	private String room = "";
	private String type;

	public AdminCommand(String address, String room, String type) {
		super();
		this.address = address;
		this.room = room;
		this.type = type;
	}

	public AdminCommand(byte[] bytes) {
		String[] s = new String(bytes).split(" ");
		type = s[0];
		address = s[1];
		if(s.length == 3){
			room = s[2];
		}
	}

	public String getAddress() {
		return address;
	}

	public String getRoom() {
		return room;
	}

	public String getType() {
		return type;
	}

	public byte[] toBytes() {
		StringBuffer sb = new StringBuffer();
		sb.append(type).append(" ").append(address).append(" ").append(room);
		return sb.toString().getBytes();
	}

	public static void main(String[] args) {
		//		AdminCommand c = new AdminCommand("sdfefw服务费违法", ADD_FOLLOWER);
		//		AdminCommand cn = new AdminCommand(c.toBytes());
		//		System.out.println(cn.getType());
		//		System.out.println(cn.getAddress());

	}
}
