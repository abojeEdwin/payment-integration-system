package com.paybridge.webhook.dto;

import com.paybridge.common.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for updating payment status in payment-service
 * <p>
 * Sent from webhook-service to payment-service via RPC call
 * <p>
 * Endpoint: PATCH /internal/payments/status
 */
@Data
@Builder
public class PaymentStatusUpdateRequest {

	/**
	 * Provider's transaction reference
	 * Used to find the payment transaction
	 */
	private String transactionReference;

	/**
	 * New status to set
	 * Values: SUCCESS, FAILED, PROCESSING, REFUNDED
	 */
	private PaymentStatus newStatus;

	/**
	 * Raw provider response (for audit trail)
	 * Stored in payment_transactions.provider_response
	 */
	private String providerResponse;

	/**
	 * Optional: Provider name for logging
	 */
	private String provider;

	/**
	 * Optional: Failure reason if status is FAILED
	 */
	private String failureReason;
}
