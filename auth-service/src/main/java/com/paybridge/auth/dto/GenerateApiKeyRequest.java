package com.paybridge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GenerateApiKeyRequest {
	@NotBlank(message = "Description is required")
	private String description;
	@Builder.Default
	private boolean live = false;
}
