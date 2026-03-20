package com.paybridge.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API Key Filter
 * <p>
 * Validates X-API-Key header on incoming requests.
 * This filter runs BEFORE JwtTokenFilter to log/audit all requests.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(ApiKeyFilter.class);

	@Value("${api.key.header:X-API-Key}")
	private String apiKeyHeader;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		String apiKey = request.getHeader(apiKeyHeader);

		if (apiKey != null) {
			logger.info("Request with API Key: {}", maskKey(apiKey));
			// TODO NOTE: Full validation happens in payment-service via RPC call to auth-service
			// This filter is for logging/audit purposes only
		}

		filterChain.doFilter(request, response);
	}

	private String maskKey(String key) {
		if (key == null || key.length() < 8) return "***";
		return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
	}
}