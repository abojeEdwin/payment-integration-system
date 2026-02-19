package com.paybridge.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "merchant_id", nullable = false)
	private Merchant merchant;

	@Column(name = "key_value", nullable = false, unique = true)
	private String keyValue; // Full API key (stored securely)

	@Column(name = "key_prefix", nullable = false)
	private String keyPrefix; // First 8 chars for display (e.g., "pk_live_a1")

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant lastUsedAt;

	/**
	 * Check if API key is valid
	 */
	public boolean isValid() {
		if (!active) return false;
		return expiresAt == null || !Instant.now().isAfter(expiresAt);
	}

	/**
	 * Mark key as used
	 */
	public void markAsUsed() {
		this.lastUsedAt = Instant.now();
	}

	/**
	 * Revoke key
	 */
	public void revoke() {
		this.active = false;
	}

	/**
	 * Generate display-friendly masked key
	 */
	public String getMaskedKey() {
		if (keyValue == null || keyValue.length() < 12) return "****";
		return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
	}
}