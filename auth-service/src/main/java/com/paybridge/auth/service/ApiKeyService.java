package com.paybridge.auth.service;

import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.entity.ApiKey;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.ApiKeyRepository;
import com.paybridge.auth.repository.MerchantRepository;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyMapper apiKeyMapper;

    private static final int KEY_LENGTH = 64;
    private static final String KEY_PREFIX_LIVE = "pk_live_";
    private static final String KEY_PREFIX_TEST = "pk_test_";

    /**
     * Generate new API key for merchant
     */
    @Transactional
    public ApiKeyResponse generateApiKey(UUID merchantId, String description, boolean isLive) {
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

        // Create API key entity
        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyValue(fullKey)
                .keyPrefix(fullKey.substring(0, 8))
                .description(description)
                .active(true)
                .expiresAt(null) // Never expires by default
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("API key generated for merchant {}: {}", merchantId, apiKey.getKeyPrefix());

        // Return full key ONLY once (never stored in DB in retrievable form)
        return ApiKeyResponse.builder()
                .apiKey(fullKey)
                .keyPrefix(apiKey.getKeyPrefix())
                .description(description)
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    /**
     * Validate API key
     */
    public Merchant validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository.findActiveByKeyValue(keyValue)
                .orElseThrow(() -> new PaymentException(
                    "Invalid or inactive API key", "AUTH_001", 
                    ErrorCategory.CLIENT_ERROR));

        // Update last used timestamp
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