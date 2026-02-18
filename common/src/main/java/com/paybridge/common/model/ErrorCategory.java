package com.paybridge.common.model;

/**
 * Business-friendly error categories (replaces HTTP status concepts).
 *
 * 📚 EDUCATION:
 * - CLIENT_ERROR: Bad request, validation, auth issues (→ 4xx)
 * - SERVER_ERROR: System failures, provider timeouts (→ 5xx)
 * - BUSINESS_ERROR: Domain-specific failures (e.g., insufficient funds → 400)
 */
public enum ErrorCategory {
	CLIENT_ERROR,      // 4xx range
	SERVER_ERROR,      // 5xx range
	BUSINESS_ERROR     // 4xx but business logic (e.g., insufficient funds)
}