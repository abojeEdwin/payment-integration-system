package com.paybridge.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyKeyGeneratorTest {

	@Test
	void generate_shouldCreateValidKey() {
		// When
		String key = IdempotencyKeyGenerator.generate();

		// Then
		assertNotNull(key);
		assertTrue(key.startsWith("idemp_"));
		assertTrue(IdempotencyKeyGenerator.isValid(key));
	}

	@Test
	void isValid_shouldReturnTrueForValidKey() {
		// Given
		String validKey = "idemp_20240115123456_abc123XYZ789ab";

		// When/Then
		assertTrue(IdempotencyKeyGenerator.isValid(validKey));
	}

	@Test
	void isValid_shouldReturnFalseForInvalidKey() {
		// Given
		String invalidKey = "invalid_key_format";

		// When/Then
		assertFalse(IdempotencyKeyGenerator.isValid(invalidKey));
	}

	@Test
	void generate_shouldProduceUniqueKeys() {
		// When
		String key1 = IdempotencyKeyGenerator.generate();
		String key2 = IdempotencyKeyGenerator.generate();

		// Then
		assertNotEquals(key1, key2);
	}
}