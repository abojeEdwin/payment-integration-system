package com.paybridge.webhook.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Generic webhook payload wrapper
 * <p>
 * Purpose: Capture raw webhook data before provider-specific parsing
 * <p>
 * This is used internally in the controller before normalization.
 * In production, you would parse this into provider-specific DTOs:
 * - PaystackWebhookPayload
 * - InterswitchWebhookPayload
 * - SquadCoWebhookPayload
 */
@Data
public class WebhookPayload {

	/**
	 * Raw JSON body as string
	 * Preserved exactly as received from provider
	 */
	private String rawJson;

	/**
	 * HTTP headers from webhook request
	 * Used for signature validation
	 */
	private Map<String, String> headers;

	/**
	 * Provider name (inferred from endpoint path)
	 * Example: "paystack", "interswitch"
	 */
	private String provider;

	/**
	 * Timestamp of receipt
	 */
	private Instant receivedAt;

	/**
	 * IP address of sender (for security logging)
	 */
	private String sourceIp;

	/**
	 * Static factory method
	 */
	public static WebhookPayload fromRequest(String rawJson, java.util.Map<String, String> headers, String provider, String sourceIp) {
		WebhookPayload payload = new WebhookPayload();
		payload.setRawJson(rawJson);
		payload.setHeaders(headers);
		payload.setProvider(provider);
		payload.setReceivedAt(Instant.now());
		payload.setSourceIp(sourceIp);
		return payload;
	}
}