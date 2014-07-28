package cn.com.sparkle.firefly.config;

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

}
