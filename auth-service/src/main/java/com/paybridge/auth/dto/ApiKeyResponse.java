package com.paybridge.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public final class ApiKeyResponse {

	private String apiKey; // Full key (returned ONLY on creation)
	private String keyPrefix; // First 8 chars (for display)
	private String description;
	private boolean active;
	private Instant expiresAt;
	private Instant createdAt;
	private Instant lastUsedAt;
}