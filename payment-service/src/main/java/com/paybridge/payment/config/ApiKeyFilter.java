package com.paybridge.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	// PHASE 1: Hardcoded test key for development ONLY
	// ⚠️ SECURITY WARNING: Replace with auth-service RPC in Phase 2
	private static final String PHASE_1_TEST_KEY = "test_api_key_123";

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		String apiKey = request.getHeader("X-API-Key");

		if (apiKey == null || apiKey.trim().isEmpty()) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\": \"Missing X-API-Key header\"}");
			return;
		}

		// PHASE 1 VALIDATION: Accept ONLY hardcoded test key
		if (!PHASE_1_TEST_KEY.equals(apiKey)) {
			log.warn("Invalid API key attempt: {}", maskKey(apiKey));
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\": \"Invalid API key (Phase 1 dev mode)\"}");
			return;
		}

		log.debug("✅ Valid Phase 1 test API key accepted");
		filterChain.doFilter(request, response);
	}

	private String maskKey(String key) {
		if (key == null || key.length() < 8) return "***";
		return key.substring(0, 4) + "****" + key.substring(Math.max(0, key.length() - 4));
	}
}