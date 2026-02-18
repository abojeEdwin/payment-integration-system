package com.paybridge.auth.controller;

import com.paybridge.auth.entity.ApiKey;
import com.paybridge.auth.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private ApiKeyService apiKeyService;

	@Test
	@WithMockUser
	void testCreateApiKey() throws Exception {
		ApiKey mockApiKey = new ApiKey();
		mockApiKey.setApiKey("test-api-key-123");
		when(apiKeyService.createApiKey(anyString())).thenReturn(mockApiKey);

		mockMvc.perform(post("/api-keys")
						.header("Authorization", "Bearer mock-jwt-token")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.apiKey").value("test-api-key-123"))
				.andExpect(jsonPath("$.merchantId").value("merchant-123"));
	}
}