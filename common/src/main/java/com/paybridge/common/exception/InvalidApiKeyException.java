package com.paybridge.common.exception;

import com.paybridge.common.model.ErrorCategory;

public class InvalidApiKeyException extends PaymentException {
	public InvalidApiKeyException(String message) {
		super(message, "AUTH_001", ErrorCategory.CLIENT_ERROR);
	}

	public InvalidApiKeyException(String apiKey, String reason) {
		super("Invalid API key [" + maskKey(apiKey) + "]: " + reason,
				"AUTH_001", ErrorCategory.CLIENT_ERROR);
	}

	// Mask key for logging (e.g., "pk_test_ab...yz")
	private static String maskKey(String key) {
		if (key == null || key.length() < 8) return "***";
		return key.substring(0, 6) + "..." + key.substring(key.length() - 2);
	}
}
