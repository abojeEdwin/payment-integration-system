package com.paybridge.common.exception;

import com.paybridge.common.model.ErrorCategory;
import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends PaymentException {

	private final Map<String, String> errors;

	public ValidationException(Map<String, String> errors) {
		super("Validation failed", "VALIDATION_001", ErrorCategory.CLIENT_ERROR);
		this.errors = errors;
	}
}
