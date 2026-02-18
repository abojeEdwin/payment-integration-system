package com.paybridge.common.exception;

import com.paybridge.common.model.ErrorCategory;
import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {
	private final String errorCode; // e.g., "AUTH_001", "PAY_002"
	private final ErrorCategory category; // NEW: Business-friendly category

	public CommonException(String message, String errorCode, ErrorCategory category) {
		super(message);
		this.errorCode = errorCode;
		this.category = category;
	}

	public CommonException(String message, Throwable cause, String errorCode, ErrorCategory category) {
		super(message, cause);
		this.errorCode = errorCode;
		this.category = category;
	}

}
