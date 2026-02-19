package com.paybridge.auth.controller;

import com.paybridge.auth.config.JwtTokenProvider;
import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.auth.service.AuthService;
import com.paybridge.common.model.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests (Security Disabled)")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private AuthService authService;

	@Mock
	private JwtTokenProvider jwtTokenProvider; // Not used in register endpoint

	private static final UUID MERCHANT_ID = UUID.randomUUID();

	@Test
	@DisplayName("POST /auth/register: Should register new merchant")
	void registerMerchant_Success() throws Exception {
		// Given
		MerchantDto merchantDto = MerchantDto.builder()
				.id(MERCHANT_ID)
				.name("New Merchant")
				.email("test@example.com")
				.paymentProvider(ProviderType.PAYSTACK)
				.active(true)
				.build();

		given(authService.registerMerchant(anyString(), anyString(), anyString(), any()))
				.willReturn(merchantDto);

		// When/Then: Pure HTTP contract test - NO security context needed
		mockMvc.perform(
						post("/auth/register")
								.param("name", "New Merchant")
								.param("email", "test@example.com")
								.param("password", "SecurePass123!")
								.param("provider", "PAYSTACK")
								.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value("test@example.com"));

		verify(authService).registerMerchant(
				eq("New Merchant"),
				eq("test@example.com"),
				eq("SecurePass123!"),
				eq(ProviderType.PAYSTACK)
		);
	}

	@Test
	@DisplayName("POST /auth/login: Should return 400 for missing fields")
	void login_ValidationFails() throws Exception {
		// When/Then: Test validation without hitting security
		mockMvc.perform(
						post("/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{}") // Empty payload
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists());
	}
}