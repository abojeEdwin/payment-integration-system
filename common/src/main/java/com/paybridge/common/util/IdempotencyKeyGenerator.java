package com.paybridge.common.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates cryptographically secure idempotency keys.
 * <p>
 * 📚 EDUCATION:
 * - Idempotency: Same request can be retried safely without duplicate effects
 * - Format: "idemp_" + timestamp + "_" + random string
 * - SecureRandom → cryptographically strong randomness
 * - Used in payment requests to prevent duplicate charges
 */
public class IdempotencyKeyGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String       CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int RANDOM_LENGTH = 16;

	/**
	 * Generate a new idempotency key
	 * Format: idemp_20240115123456_abc123XYZ789
	 */
	public static String generate() {
		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String random = generateRandomString();
		return "idemp_" + timestamp + "_" + random;
	}

	/**
	 * Validate if a string is a valid idempotency key
	 */
	public static boolean isValid(String key) {
		if (key == null) return false;
		return key.matches("^idemp_\\d{14}_[A-Za-z0-9]{16}$");
	}

	private static String generateRandomString() {
		StringBuilder sb = new StringBuilder(RANDOM_LENGTH);
		for (int i = 0; i < RANDOM_LENGTH; i++) {
			sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
		}
		return sb.toString();
	}
}