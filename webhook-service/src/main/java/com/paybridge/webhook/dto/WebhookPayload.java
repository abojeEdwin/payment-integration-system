package com.paybridge.webhook.dto;

public record WebhookPayload(
		String provider,
		String rawPayload
) {
}