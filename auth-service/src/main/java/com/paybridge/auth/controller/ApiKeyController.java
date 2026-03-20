package com.paybridge.auth.controller;


import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.dto.ApiKeyValidationResponse;
import com.paybridge.auth.dto.GenerateApiKeyRequest;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.service.ApiKeyService;
import com.paybridge.common.dto.ApiResponse;
import com.paybridge.common.exception.PaymentException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

	private final ApiKeyService apiKeyService;
	/**
	 * Generate new API key
	 * POST /api-keys
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<ApiKeyResponse>> generateApiKey(
			Authentication authentication,
			@RequestBody GenerateApiKeyRequest request) {

		UUID merchantId = UUID.fromString(authentication.getName());

		ApiKeyResponse response = apiKeyService.generateApiKey(merchantId, request);
		return ResponseEntity.ok(ApiResponse.success(response,
				"API key generated successfully. Save this key now - it won't be shown again!"));
	}

	/**
	 * Get all API keys for merchant
	 * GET /api-keys
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getMerchantApiKeys(
			Authentication authentication) {

		UUID merchantId = UUID.fromString(authentication.getName());
		List<ApiKeyResponse> keys = apiKeyService.getMerchantApiKeys(merchantId);
		return ResponseEntity.ok(ApiResponse.success(keys));
	}

	/**
	 * Revoke API key by ID
	 * DELETE /api-keys/{keyId}
	 */
	@DeleteMapping("/{keyId}")
	public ResponseEntity<ApiResponse<Void>> revokeApiKey(
			@PathVariable UUID keyId) {

		apiKeyService.revokeApiKey(keyId);
		return ResponseEntity.ok(ApiResponse.<Void>builder()
				.message("API key revoked successfully")
				.build());
	}

	/**
	 * Revoke API key by prefix
	 * DELETE /api-keys/prefix/{prefix}
	 */
	@DeleteMapping("/prefix/{prefix}")
	public ResponseEntity<ApiResponse<Void>> revokeApiKeyByPrefix(
			Authentication authentication,
			@PathVariable String prefix) {

		UUID merchantId = UUID.fromString(authentication.getName());
		apiKeyService.revokeApiKeyByPrefix(merchantId, prefix);
		return ResponseEntity.ok(ApiResponse.<Void>builder()
				.message("API key revoked successfully")
				.build());
	}

	//TODO i don't want to pass key via path,
	// instead i want to pass it via header, but for testing purpose i am
	// passing it via path
	// also i would want to use my response dto for thie,
	// ApiResponse<ApiKeyValidationResponse> instead of just ApiKeyValidationResponse

	@GetMapping("/validate")
	public ResponseEntity<ApiKeyValidationResponse> validateApiKey(
			@RequestHeader("X-API-Key")
			@NotBlank(message = "API key not found in 'X-API-Key' header.")
			String apiKey) {

		try {
			Merchant merchant = apiKeyService.validateApiKey(apiKey);
			return ResponseEntity.ok(ApiKeyValidationResponse.builder()
					.merchantId(merchant.getId().toString())
					.isActive(merchant.isActive())
					.provider(merchant.getPaymentProvider())
					.build());
		} catch (PaymentException ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}
}