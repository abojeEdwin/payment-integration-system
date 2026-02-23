package com.paybridge.webhook.exception;

public class WebhookProcessingException extends RuntimeException {
	public WebhookProcessingException(String message) {
		super(message);
	}
}
