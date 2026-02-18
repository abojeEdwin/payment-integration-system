package com.paybridge.auth.exception;

public class MerchantNotFoundException extends RuntimeException {
	public MerchantNotFoundException(String message) {
		super(message);
	}
}
