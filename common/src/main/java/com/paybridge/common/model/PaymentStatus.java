package com.paybridge.common.model;

public enum PaymentStatus {
	/**
	 * Payment request received, awaiting processing
	 */
	INITIATED,

	/**
	 * Payment is being processed by provider
	 */
	PROCESSING,

	/**
	 * Payment succeeded
	 */
	SUCCESS,

	/**
	 * Payment failed (insufficient funds, declined, etc.)
	 */
	FAILED,

	/**
	 * Payment was refunded
	 */
	REFUNDED,

	/**
	 * Payment was cancelled before completion
	 */
	CANCELLED;

	/**
	 * Check if status is terminal (no further state changes expected)
	 */
	public boolean isFinal() {
		return this == SUCCESS || this == FAILED || this == REFUNDED || this == CANCELLED;
	}
}
