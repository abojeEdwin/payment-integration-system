package com.paybridge.common.exception;

public class InvalidApiKeyException extends RuntimeException {
	public InvalidApiKeyException(String message) {
		super(message);
	}
}
