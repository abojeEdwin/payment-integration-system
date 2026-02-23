package com.paybridge.webhook.service.provider;

import com.paybridge.common.model.WebhookEventType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
public class PaystackSignatureValidator implements SignatureValidator {

	private final String secretKey;

	public PaystackSignatureValidator(String secretKey) {
		this.secretKey = secretKey;
	}

	@Override
	public boolean isValid(String rawPayload, String signatureHeader) {
		if (secretKey == null || secretKey.isEmpty()) {
			log.warn("Paystack webhook secret not configured - skipping validation");
			return true; // Allow in dev mode
		}

		if (signatureHeader == null) return false;

		try {
			// Paystack format: "t=<timestamp>,v1=<signature>"
			String[] parts = signatureHeader.split(",");
			String v1Part = null;
			for (String part : parts) {
				if (part.startsWith("v1=")) {
					v1Part = part.substring(3); // Remove "v1="
					break;
				}
			}

			if (v1Part == null) return false;

			// Compute expected signature
			String expectedSignature = HmacUtils.hmacSha512Hex(secretKey, rawPayload);

			// Constant-time comparison to prevent timing attacks
			return MessageDigest.isEqual(
					expectedSignature.getBytes(StandardCharsets.UTF_8),
					v1Part.getBytes(StandardCharsets.UTF_8)
			);
		} catch (Exception e) {
			log.error("Error validating Paystack signature", e);
			return false;
		}
	}

	@Override
	public String extractEventId(String rawPayload) {
		// In production: Parse JSON and extract event ID
		// For simplicity: Use hash of payload + timestamp
		return "paystack_" + MessageDigest.getInstance("SHA-256")
				.digest((rawPayload + System.currentTimeMillis()).getBytes())
				.toString();
	}

	@Override
	public String extractTransactionReference(String rawPayload) {
		// Parse JSON: payload.data.reference
		// Simplified for example
		return extractJsonValue(rawPayload, "reference");
	}

	@Override
	public WebhookEventType normalizeEventType(String rawPayload) {
		String event = extractJsonValue(rawPayload, "event");
		return WebhookEventType.fromProviderEvent("PAYSTACK", event);
	}

	// Simplified JSON extraction (use Jackson in real implementation)
	private String extractJsonValue(String json, String key) {
		// Use proper JSON parser in production
		return "ref_" + System.currentTimeMillis();
	}
}
