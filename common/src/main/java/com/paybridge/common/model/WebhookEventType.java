package com.paybridge.common.model;

/**
 * Standardized webhook event types across providers.
 * <p>
 * 📚 EDUCATION:
 * - Normalize provider-specific events (e.g., "charge.success" → PAYMENT_SUCCESS)
 * - Use fromProviderEvent() to map raw provider events to standard types
 */
public enum WebhookEventType {

	PAYMENT_SUCCESS,
	PAYMENT_FAILED,
	PAYMENT_PENDING,
	REFUND_SUCCESS,
	REFUND_FAILED,
	SUBSCRIPTION_CREATED,
	SUBSCRIPTION_CANCELLED,
	UNKNOWN; // Fallback for unmapped events

	/**
	 * Map provider-specific event string to standard type
	 * Example: "charge.success" (Paystack) → PAYMENT_SUCCESS
	 */
	public static WebhookEventType fromProviderEvent(String provider, String rawEvent) {
		if (provider == null || rawEvent == null) return UNKNOWN;

//		I should do this in a more scalable way, but for now this is fine.
//		In the future, consider using a configuration file or database to
//		map provider events to standard types without hardcoding.
		return switch (provider.toUpperCase()) {
			case "PAYSTACK" -> switch (rawEvent) {
				case "charge.success" -> PAYMENT_SUCCESS;
				case "charge.failed" -> PAYMENT_FAILED;
				case "charge.pending" -> PAYMENT_PENDING;
				default -> UNKNOWN;
			};
			case "INTERSWITCH" -> switch (rawEvent) {
				case "transaction.success" -> PAYMENT_SUCCESS;
				case "transaction.failed" -> PAYMENT_FAILED;
				default -> UNKNOWN;
			};
			default -> UNKNOWN;
		};
	}
}