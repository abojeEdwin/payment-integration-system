package com.paybridge.auth.dto;

import com.paybridge.common.model.ProviderType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiKeyValidationResponse {
	private String       merchantId;
	private boolean      isActive;
	private ProviderType provider;
}
