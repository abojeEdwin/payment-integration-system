package com.paybridge.webhook.entity;

import com.paybridge.common.model.WebhookEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", indexes = {
		@Index(name = "idx_provider", columnList = "provider"),
		@Index(name = "idx_status", columnList = "status"),
		@Index(name = "idx_external_ref", columnList = "external_reference"),
		@Index(name = "idx_received_at", columnList = "received_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 50)
	private String provider; // "paystack", "interswitch", etc.

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WebhookEventType eventType;

	@Column(name = "external_reference", length = 255)
	private String externalReference; // Provider's transaction ID

	@Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
	private String rawPayload; // Full JSON payload (immutable audit log)

	@Column(name = "signature_header", length = 500)
	private String signatureHeader; // Original signature header

	@Column(length = 20, nullable = false)
	private String status; // PENDING, PROCESSING, PROCESSED, FAILED, INVALID_SIGNATURE

	@Column(name = "failure_reason", columnDefinition = "TEXT")
	private String failureReason;

	@CreationTimestamp
	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@UpdateTimestamp
	@Column(name = "processed_at")
	private Instant processedAt;

	// Helper methods
	public void markAsProcessed() {
		this.status = "PROCESSED";
		this.processedAt = Instant.now();
	}

	public void markAsFailed(String reason) {
		this.status = "FAILED";
		this.failureReason = reason;
		this.processedAt = Instant.now();
	}

	public void markAsInvalidSignature() {
		this.status = "INVALID_SIGNATURE";
		this.processedAt = Instant.now();
	}
}