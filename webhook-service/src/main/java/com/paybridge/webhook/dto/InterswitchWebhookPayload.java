package com.paybridge.webhook.dto;

import lombok.Data;

/**
 * Interswitch webhook payload structure
 *
 * Example payload from Interswitch:
 * {
 *   "transactionReference": "ISW_REF_123",
 *   "amount": 150000,
 *   "responseCode": "00",
 *   "responseDescription": "Approved",
 *   "paymentStatus": "SUCCESS"
 * }
 */
@Data
public class InterswitchWebhookPayload {

	private String transactionReference;
	private Integer amount; // In kobo
	private String responseCode; // "00" = success
	private String responseDescription;
	private String paymentStatus; // "SUCCESS", "FAILED"
	private String customerId;
	private java.time.Instant transactionDate;
}