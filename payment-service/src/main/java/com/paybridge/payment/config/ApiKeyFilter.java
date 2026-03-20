package com.paybridge.payment.config;

import com.paybridge.payment.client.AuthClient;
import com.paybridge.payment.dto.MerchantInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

	private final AuthClient authClient;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.startsWith("/actuator") || path.startsWith("/error");
	}

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

		MerchantInfo merchant = authClient.validateApiKey(apiKey);

		if (merchant == null || !merchant.isActive() || merchant.getMerchantId() == null) {
			log.warn("Invalid API key attempt: {}", maskKey(apiKey));
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\": \"Invalid API key\"}");
			return;
		}
		// Store merchant info in request for controller access
		request.setAttribute("MERCHANT_ID", merchant.getMerchantId());
		request.setAttribute("PROVIDER", merchant.getProvider());

		log.debug("✅ Valid API key for merchant: {}", merchant.getMerchantId());
		filterChain.doFilter(request, response);
	}

	private String maskKey(String key) {
		if (key == null || key.length() < 8) return "***";
		return key.substring(0, 4) + "****" + key.substring(Math.max(0, key.length() - 4));
	}
}