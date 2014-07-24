package cn.com.sparkle.paxos.client;

public class MasterLostException extends Exception {
	private static final long serialVersionUID = 8355033853077474930L;

	public MasterLostException(String message) {
		super(message);
	}

}
