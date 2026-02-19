package com.paybridge.auth.service;

import com.paybridge.auth.entity.ApiKey;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.ApiKeyRepository;
import com.paybridge.auth.repository.MerchantRepository;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Unit Tests")
class ApiKeyServiceTest {

	@Mock
	private ApiKeyRepository apiKeyRepository;

	@Mock
	private MerchantRepository merchantRepository;

	@Mock
	private ApiKeyMapper apiKeyMapper;

	@InjectMocks
	private ApiKeyService apiKeyService;

	private Merchant testMerchant;
	private UUID merchantId;

	@BeforeEach
	void setUp() {
		merchantId = UUID.randomUUID();
		testMerchant = Merchant.builder()
				.id(merchantId)
				.name("Test Merchant")
				.email("test@example.com")
				.paymentProvider(ProviderType.PAYSTACK)
				.active(true)
				.build();
	}

	// ==================== GENERATE API KEY ====================

	@Test
	@DisplayName("generateApiKey: Should generate new key when merchant exists and under limit")
	void generateApiKey_Success() {
		// Given
		given(merchantRepository.findById(merchantId)).willReturn(Optional.of(testMerchant));
		given(apiKeyRepository.countByMerchantIdAndActive(merchantId, true)).willReturn(5L);
		given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(invocation -> {
			ApiKey saved = invocation.getArgument(0);
			saved.setId(UUID.randomUUID()); // Simulate DB save
			return saved;
		});

		// When
		var result = apiKeyService.generateApiKey(merchantId, "Test Key", true);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getKeyValue()).startsWith("pk_live_");
		assertThat(result.getKeyPrefix()).hasSize(8);
		verify(apiKeyRepository).save(any(ApiKey.class));
	}

	@Test
	@DisplayName("generateApiKey: Should throw exception when merchant not found")
	void generateApiKey_MerchantNotFound() {
		// Given
		given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> apiKeyService.generateApiKey(merchantId, "Test", true))
				.isInstanceOf(PaymentException.class)
				.hasFieldOrPropertyWithValue("errorCode", "AUTH_005");

		verify(apiKeyRepository, never()).save(any());
	}

	@Test
	@DisplayName("generateApiKey: Should throw exception when key limit reached")
	void generateApiKey_KeyLimitReached() {
		// Given
		given(merchantRepository.findById(merchantId)).willReturn(Optional.of(testMerchant));
		given(apiKeyRepository.countByMerchantIdAndActive(merchantId, true)).willReturn(10L);

		// When/Then
		assertThatThrownBy(() -> apiKeyService.generateApiKey(merchantId, "Test", true))
				.isInstanceOf(PaymentException.class)
				.hasFieldOrPropertyWithValue("errorCode", "AUTH_006");
	}

	// ==================== VALIDATE API KEY ====================

	@Test
	@DisplayName("validateApiKey: Should return merchant for valid active key")
	void validateApiKey_Success() {
		// Given
		String validKey = "pk_live_" + generateRandomString(56);
		ApiKey apiKey = ApiKey.builder()
				.id(UUID.randomUUID())
				.merchant(testMerchant)
				.keyValue(validKey)
				.keyPrefix(validKey.substring(0, 8))
				.active(true)
				.lastUsedAt(Instant.now())
				.build();

		given(apiKeyRepository.findActiveByKeyValue(validKey)).willReturn(Optional.of(apiKey));
		given(apiKeyRepository.save(any(ApiKey.class))).willReturn(apiKey);

		// When
		Merchant merchant = apiKeyService.validateApiKey(validKey);

		// Then
		assertThat(merchant).isEqualTo(testMerchant);
		verify(apiKeyRepository).save(argThat(k ->
				k.getLastUsedAt().isAfter(Instant.now().minusSeconds(1))
		));
	}

	@Test
	@DisplayName("validateApiKey: Should throw exception for invalid key")
	void validateApiKey_InvalidKey() {
		// Given
		given(apiKeyRepository.findActiveByKeyValue("invalid_key")).willReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> apiKeyService.validateApiKey("invalid_key"))
				.isInstanceOf(PaymentException.class)
				.hasFieldOrPropertyWithValue("errorCode", "AUTH_001");
	}

	// ==================== REVOKE API KEY ====================

	@Test
	@DisplayName("revokeApiKey: Should deactivate key when found")
	void revokeApiKey_Success() {
		// Given
		UUID keyId = UUID.randomUUID();
		ApiKey apiKey = ApiKey.builder()
				.id(keyId)
				.merchant(testMerchant)
				.keyValue("pk_test_abc")
				.active(true)
				.build();

		given(apiKeyRepository.findById(keyId)).willReturn(Optional.of(apiKey));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(invocation -> invocation.getArgument(0));

		// When
		apiKeyService.revokeApiKey(keyId);

		//Then: Verify save was called AND the key was deactivated
		ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
		verify(apiKeyRepository).save(captor.capture());

		ApiKey savedKey = captor.getValue();
		assertThat(savedKey).isNotNull();
		assertThat(savedKey.isActive()).isFalse();
		assertThat(savedKey.getId()).isEqualTo(keyId);
	}

	@Test
	@DisplayName("revokeApiKey: Should throw exception when key not found")
	void revokeApiKey_NotFound() {
		// Given
		given(apiKeyRepository.findById(any(UUID.class))).willReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> apiKeyService.revokeApiKey(UUID.randomUUID()))
				.isInstanceOf(PaymentException.class)
				.hasFieldOrPropertyWithValue("errorCode", "AUTH_007");
	}

	// ==================== HELPER ====================

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