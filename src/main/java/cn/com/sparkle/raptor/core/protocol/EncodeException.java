package cn.com.sparkle.raptor.core.protocol;

import java.io.IOException;

public class EncodeException extends IOException {

	private static final long serialVersionUID = 1L;

	public EncodeException() {
		super();
	}

	// public EncodeException(String message, Throwable cause,
	// boolean enableSuppression, boolean writableStackTrace) {
	// super(message, cause, enableSuppression, writableStackTrace);
	// }

	public EncodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public EncodeException(String message) {
		super(message);
	}

	public EncodeException(Throwable cause) {
		super(cause);
	}

}
