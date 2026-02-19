package com.paybridge.auth.controller;

import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.service.ApiKeyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiKeyController.class)
@DisplayName("ApiKeyController Web Layer Tests")
class ApiKeyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private ApiKeyService apiKeyService;

	private static final String MERCHANT_ID   = "123e4567-e89b-12d3-a456-426614174000";
	private static final UUID   MERCHANT_UUID = UUID.fromString(MERCHANT_ID);

	// ==================== GENERATE KEY ====================

	@Test
	@DisplayName("POST /api-keys: Should generate new API key")
	void generateApiKey_Success() throws Exception {
		// Given
		ApiKeyResponse response = ApiKeyResponse.builder()
				.keyValue("pk_live_" + "x".repeat(56))
				.keyPrefix("pk_live_x")
				.description("Test Key")
				.active(true)
				.createdAt(Instant.now())
				.build();

		given(apiKeyService.generateApiKey(eq(MERCHANT_UUID), anyString(), anyBoolean()))
				.willReturn(response);

		// When
		ResultActions result = mockMvc.perform(
				post("/api-keys")
						.param("description", "Test Key")
						.param("live", "true")
//						.with(csrf()) // Required since we disabled security filters
						.contentType(MediaType.APPLICATION_JSON)
		);

		// Then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.apiKey").exists())
				.andExpect(jsonPath("$.data.keyPrefix").value("pk_live_x"));

		verify(apiKeyService).generateApiKey(eq(MERCHANT_UUID), eq("Test Key"), eq(true));
	}

	// ==================== GET KEYS ====================

	@Test
	@WithMockUser(username = MERCHANT_ID, authorities = {"ROLE_MERCHANT"})
	@DisplayName("GET /api-keys: Should return list of API keys")
	void getMerchantApiKeys_Success() throws Exception {
		// Given
		List<ApiKeyResponse> keys = List.of(
				ApiKeyResponse.builder()
						.keyPrefix("pk_test_a")
						.description("Test Key 1")
						.active(true)
						.build()
		);

		given(apiKeyService.getMerchantApiKeys(MERCHANT_UUID)).willReturn(keys);

		// When/Then
		mockMvc.perform(get("/api-keys"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
	}

	// ==================== REVOKE KEY ====================

	@Test
	@DisplayName("DELETE /api-keys/{id}: Should revoke API key")
	void revokeApiKey_Success() throws Exception {
		// Given
		UUID keyId = UUID.randomUUID();

		// When/Then
		mockMvc.perform(delete("/api-keys/" + keyId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("API key revoked successfully"));

		verify(apiKeyService).revokeApiKey(eq(keyId));
	}
}