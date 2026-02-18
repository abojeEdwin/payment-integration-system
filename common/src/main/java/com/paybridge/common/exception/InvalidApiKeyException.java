package com.paybridge.common.exception;

public class InvalidApiKeyException extends CommonException {
	public InvalidApiKeyException(String message) {
		super(message, org.springframework.http.HttpStatus.UNAUTHORIZED, "AUTH_001");
	}

	public InvalidApiKeyException(String apiKey, String reason) {
		super("Invalid API key [" + maskKey(apiKey) + "]: " + reason,
				org.springframework.http.HttpStatus.UNAUTHORIZED, "AUTH_001");
	}

	// Mask key for logging (e.g., "pk_test_ab...yz")
	private static String maskKey(String key) {
		if (key == null || key.length() < 8) return "***";
		return key.substring(0, 6) + "..." + key.substring(key.length() - 2);
	}
}
