package com.paybridge.common.exception;

import java.util.Map;

public class ValidationException extends CommonException {

	private final Map<String, String> errors;

	public ValidationException(Map<String, String> errors) {
		super("Validation failed", org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_001");
		this.errors = errors;
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
