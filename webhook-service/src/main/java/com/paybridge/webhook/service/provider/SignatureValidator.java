package com.paybridge.webhook.service.provider;


import com.paybridge.common.model.WebhookEventType;

/**
 * Strategy interface for provider-specific signature validation
 */
public interface SignatureValidator {

	/**
	 * Validate webhook signature
	 * @param rawPayload Raw JSON body
	 * @param signatureHeader Full signature header (e.g., "t=123,v1=abc")
	 * @return true if valid
	 */
	boolean isValid(String rawPayload, String signatureHeader);

	/**
	 * Extract event ID for idempotency tracking
	 * @param rawPayload Raw JSON body
	 * @return Unique event identifier
	 */
	String extractEventId(String rawPayload);

	/**
	 * Extract transaction reference from payload
	 */
	String extractTransactionReference(String rawPayload);

	/**
	 * Normalize event type to internal enum
	 */
	WebhookEventType normalizeEventType(String rawPayload);
}