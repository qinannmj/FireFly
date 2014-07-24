package cn.com.sparkle.raptor.core.transport.socket.nio.exception;

public class SessionHavaClosedException extends Exception {

	private static final long serialVersionUID = 4345724675941666205L;

	public SessionHavaClosedException() {
		super();
	}

	public SessionHavaClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionHavaClosedException(String message) {
		super(message);
	}

	public SessionHavaClosedException(Throwable cause) {
		super(cause);
	}

}
