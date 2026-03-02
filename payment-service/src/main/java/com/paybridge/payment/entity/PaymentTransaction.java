package com.paybridge.payment.entity;


import com.paybridge.common.model.Currency;
import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions", indexes = {
		@Index(name = "idx_merchant_id", columnList = "merchantId"),
		@Index(name = "idx_external_ref", columnList = "externalReference", unique = true),
		@Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
		@Index(name = "idx_api_key", columnList = "apiKey")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID merchantId;

	@Column(nullable = false)
	private String apiKey; // Simplified: store API key directly (Phase 1)

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Currency currency;

	@Column(nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProviderType provider;

	@Column(unique = true, nullable = false)
	private String externalReference; // Provider's transaction ID (e.g., Paystack reference)

	@Column(unique = true, nullable = false)
	private String idempotencyKey; // Critical for duplicate prevention

	@Column(columnDefinition = "TEXT")
	private String providerResponse; // Raw JSON response from provider

	@Column(columnDefinition = "TEXT")
	private String failureReason;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	/**
	 * Check if this transaction is idempotent match for a new request
	 */
	public boolean matchesIdempotencyKey(String key) {
		return this.idempotencyKey.equals(key);
	}

	/**
	 * Update status with audit trail
	 */
	public void updateStatus(PaymentStatus newStatus, String providerResponse) {
		this.status = newStatus;
		this.providerResponse = providerResponse;
		if (newStatus == PaymentStatus.FAILED) {
			this.failureReason = extractFailureReason(providerResponse);
		}
	}

	private String extractFailureReason(String response) {
		// Simplified - in real system parse provider-specific response
		return "Payment failed at provider";
	}
}