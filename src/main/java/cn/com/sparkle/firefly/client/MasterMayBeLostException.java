package cn.com.sparkle.firefly.client;

public class MasterMayBeLostException extends Exception {

	private static final long serialVersionUID = 1L;

	public MasterMayBeLostException() {
		super("The master may be lost!");
	}
}
