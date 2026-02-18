package com.paybridge.common.exception;

public class CommonException extends RuntimeException {
	private final org.springframework.http.HttpStatus status;
	private final String errorCode;

	public CommonException(String message, org.springframework.http.HttpStatus status, String errorCode) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}

	public CommonException(String message, Throwable cause, org.springframework.http.HttpStatus status, String errorCode) {
		super(message, cause);
		this.status = status;
		this.errorCode = errorCode;
	}

	public org.springframework.http.HttpStatus getStatus() {
		return status;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
