package com.paybridge.auth.exception;

public class ApiKeyRevokedException extends RuntimeException {
	public ApiKeyRevokedException(String message) {
		super(message);
	}
}
