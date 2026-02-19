package com.paybridge.auth.service;


import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.MerchantRepository;
import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private MerchantRepository merchantRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private MerchantMapper merchantMapper;

	@InjectMocks
	private AuthService authService;

	private Merchant testMerchant;

	@BeforeEach
	void setUp() {
		testMerchant = Merchant.builder()
				.id(UUID.randomUUID())
				.name("Test Merchant")
				.email("test@example.com")
				.passwordHash("$2a$10$encryptedhash")
				.paymentProvider(ProviderType.PAYSTACK)
				.active(true)
				.build();
	}

	@Test
	void registerMerchant_shouldCreateNewMerchant() {
		// Given
		String name = "New Merchant";
		String email = "new@example.com";
		String password = "password123";
		ProviderType provider = ProviderType.PAYSTACK;

		when(merchantRepository.existsByEmail(email)).thenReturn(false);
		when(passwordEncoder.encode(password)).thenReturn("$2a$10$encryptedhash");
		when(merchantRepository.save(any(Merchant.class))).thenReturn(testMerchant);
		when(merchantMapper.toDto(testMerchant)).thenReturn(MerchantDto.builder()
				.id(testMerchant.getId())
				.name(name)
				.email(email)
				.build());

		// When
		MerchantDto result = authService.registerMerchant(name, email, password, provider);

		// Then
		assertNotNull(result);
		assertEquals(email, result.getEmail());
		verify(merchantRepository).save(any(Merchant.class));
	}

	@Test
	void registerMerchant_shouldThrowExceptionWhenEmailExists() {
		// Given
		when(merchantRepository.existsByEmail("existing@example.com")).thenReturn(true);

		// When/Then
		PaymentException exception = assertThrows(PaymentException.class, () ->
				authService.registerMerchant("Test", "existing@example.com", "pass", ProviderType.PAYSTACK));

		assertEquals("AUTH_002", exception.getErrorCode());
	}

	@Test
	void authenticate_shouldReturnMerchantWhenCredentialsValid() {
		// Given
		String email = "test@example.com";
		String password = "password123";

		when(merchantRepository.findActiveByEmail(email)).thenReturn(Optional.of(testMerchant));
		when(passwordEncoder.matches(password, testMerchant.getPasswordHash())).thenReturn(true);

		// When
		Merchant result = authService.authenticate(email, password);

		// Then
		assertNotNull(result);
		assertEquals(email, result.getEmail());
	}

	@Test
	void authenticate_shouldThrowExceptionWhenPasswordInvalid() {
		// Given
		when(merchantRepository.findActiveByEmail("test@example.com")).thenReturn(Optional.of(testMerchant));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		// When/Then
		PaymentException exception = assertThrows(PaymentException.class, () ->
				authService.authenticate("test@example.com", "wrongpassword"));

		assertEquals("AUTH_003", exception.getErrorCode());
	}
}