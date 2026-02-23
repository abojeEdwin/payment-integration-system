package com.paybridge.webhook.dto;

import lombok.Data;

import java.util.Map;

/**
 * Paystack webhook payload structure
 * <p>
 * Example payload from Paystack:
 * {
 *   "event": "charge.success",
 *   "data": {
 *     "id": 123456789,
 *     "reference": "txn_abc123",
 *     "amount": 150000,
 *     "currency": "NGN",
 *     "status": "success",
 *     "customer": { "email": "customer@example.com" }
 *   }
 * }
 */
@Data
public class PaystackWebhookPayload {

	private String event; // "charge.success", "charge.failed", etc.

	private PaystackWebhookData data;

	@Data
	public static class PaystackWebhookData {
		private Long id;
		private String reference; // Transaction reference
		private Integer amount; // In kobo (100 kobo = 1 NGN)
		private String currency; // "NGN"
		private String status; // "success", "failed"
		private PaystackCustomer    customer;
		private Map<String, Object> metadata;
	}

	@Data
	public static class PaystackCustomer {
		private String email;
		private String firstName;
		private String lastName;
	}
}
