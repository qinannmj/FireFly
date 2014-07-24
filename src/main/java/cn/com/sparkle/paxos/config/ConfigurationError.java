package cn.com.sparkle.paxos.config;

public class ConfigurationError extends RuntimeException {

	private static final long serialVersionUID = 3256785277440743090L;

	public ConfigurationError() {
	}

	public ConfigurationError(String message) {
		super(message);
	}

	public ConfigurationError(Throwable cause) {
		super(cause);
	}

	public ConfigurationError(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
