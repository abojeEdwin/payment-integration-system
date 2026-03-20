package com.paybridge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public final class CreateApiKeyRequest {

	@NotBlank(message = "Description is required")
	private String description;

	private boolean live = false;
}