package cn.com.sparkle.paxos.stablestorage;

public class FileDamageException extends Exception {

	private static final long serialVersionUID = -214509181565776614L;

	public FileDamageException(String message) {
		super(message);
	}
}
