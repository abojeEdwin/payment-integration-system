package com.paybridge.auth.service;

import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.dto.GenerateApiKeyRequest;
import com.paybridge.auth.entity.ApiKey;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.ApiKeyRepository;
import com.paybridge.auth.repository.MerchantRepository;
import com.paybridge.common.exception.InvalidApiKeyException;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Service
public class ApiKeyService {

	private final ApiKeyRepository   apiKeyRepository;
	private final MerchantRepository merchantRepository;
	private final ApiKeyMapper       apiKeyMapper;
	private final SecretKeySpec      hmacKeySpec;

	public ApiKeyService(ApiKeyRepository apiKeyRepository,
						 MerchantRepository merchantRepository,
						 ApiKeyMapper apiKeyMapper,
						 @Value("${api.key.hmac-secret}") String hmacSecret) {
		this.apiKeyRepository   = apiKeyRepository;
		this.merchantRepository = merchantRepository;
		this.apiKeyMapper       = apiKeyMapper;
		this.hmacKeySpec        = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	private static final int    KEY_LENGTH      = 64;
	private static final String KEY_PREFIX_LIVE = "pk_live_";
	private static final String KEY_PREFIX_TEST = "pk_test_";

	/**
	 * Generate new API key for merchant
	 */
	@Transactional
	public ApiKeyResponse generateApiKey(UUID merchantId, GenerateApiKeyRequest request) {
		String description = request.getDescription();
		boolean isLive = request.isLive();

		// Get merchant
		Merchant merchant = merchantRepository.findById(merchantId)
				.orElseThrow(() -> new PaymentException(
						"Merchant not found", "AUTH_005",
						ErrorCategory.CLIENT_ERROR));

		// Enforce key limits (e.g., max 10 active keys per merchant)
		long activeKeyCount = apiKeyRepository.countByMerchantIdAndActive(merchantId, true);
		if (activeKeyCount >= 10) {
			throw new PaymentException(
					"Maximum number of active API keys reached (10)", "AUTH_006",
					ErrorCategory.CLIENT_ERROR);
		}

		// Generate secure random key
		String fullKey = generateSecureKey(isLive);

		// Create API key entity — store HMAC-SHA-256 hash, never the raw key
		ApiKey apiKey = ApiKey.builder()
				.merchant(merchant)
				.keyValue(hashKey(fullKey))
				.keyPrefix(fullKey.substring(0, 8))
				.description(description)
				.active(true)
				.expiresAt(null) // Never expires by default
				.build();

		apiKey = apiKeyRepository.save(apiKey);
		log.info("API key generated for merchant {}: {}", merchantId, apiKey.getKeyPrefix());

		// Return full key ONLY once (never stored in DB in retrievable form)
		return ApiKeyResponse.builder()
				.keyValue(fullKey)
				.keyPrefix(apiKey.getKeyPrefix())
				.description(description)
				.active(true)
				.expiresAt(apiKey.getExpiresAt())
				.createdAt(apiKey.getCreatedAt())
				.lastUsedAt(apiKey.getLastUsedAt())
				.build();
	}

	/**
	 * Hash API key using HMAC-SHA-256 with a server-side secret for secure, deterministic storage.
	 * Unlike BCrypt, HMAC-SHA-256 is deterministic (same input + same secret → same hash), so
	 * the hashed value can be stored in an indexed unique column and looked up directly — O(1)
	 * retrieval, no sequential scan needed.
	 * The server-side secret acts as a pepper: even if the DB is compromised, an attacker
	 * cannot brute-force the keys without also knowing the secret.
	 */
	private String hashKey(String keyValue) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(hmacKeySpec);
			byte[] hashBytes = mac.doFinal(keyValue.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("HMAC-SHA-256 computation failed", e);
		}
	}

	/**
	 * Validate API key
	 */
	public Merchant validateApiKey(String keyValue) throws PaymentException {
		ApiKey apiKey = apiKeyRepository.findByKeyValueAndActiveTrue(hashKey(keyValue))
				.orElseThrow(() -> new InvalidApiKeyException(
						keyValue, "API key not found or inactive"));

        apiKey.markAsUsed();
        apiKeyRepository.save(apiKey);
        return apiKey.getMerchant();
	}

	/**
	 * Get all API keys for merchant
	 */
	public List<ApiKeyResponse> getMerchantApiKeys(UUID merchantId) {
		List<ApiKey> apiKeys = apiKeyRepository.findByMerchantId(merchantId);
		return apiKeys.stream()
				.map(apiKeyMapper::toResponse)
				.toList();
	}

	/**
	 * Revoke API key
	 */
	@Transactional
	public void revokeApiKey(UUID apiKeyId) {
		ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
				.orElseThrow(() -> new PaymentException(
						"API key not found", "AUTH_007",
						ErrorCategory.CLIENT_ERROR));

		apiKey.revoke();
		apiKeyRepository.save(apiKey);
		log.info("API key revoked: {}", apiKey.getKeyPrefix());
	}

	/**
	 * Revoke API key by prefix (for merchant self-service)
	 */
	@Transactional
	public void revokeApiKeyByPrefix(UUID merchantId, String keyPrefix) {
		List<ApiKey> apiKeys = apiKeyRepository.findByMerchantId(merchantId);

		boolean revoked = false;
		for (ApiKey apiKey : apiKeys) {
			if (apiKey.getKeyPrefix().equals(keyPrefix) && apiKey.isActive()) {
				apiKey.revoke();
				apiKeyRepository.save(apiKey);
				revoked = true;
				log.info("API key revoked by prefix: {}", keyPrefix);
				break;
			}
		}

		if (!revoked) {
			throw new PaymentException(
					"Active API key with prefix not found", "AUTH_008",
					ErrorCategory.CLIENT_ERROR);
		}
	}

	/**
	 * Generate cryptographically secure API key
	 */
	private String generateSecureKey(boolean isLive) {
		String prefix = isLive ? KEY_PREFIX_LIVE : KEY_PREFIX_TEST;
		String randomPart = generateRandomString(KEY_LENGTH - prefix.length());
		return prefix + randomPart;
	}

	private String generateRandomString(int length) {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder(length);
		java.security.SecureRandom random = new java.security.SecureRandom();

		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}

		return sb.toString();
	}
}