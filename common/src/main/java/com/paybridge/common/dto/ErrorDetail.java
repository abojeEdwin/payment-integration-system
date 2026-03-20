package com.paybridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 7807 Problem Details for HTTP APIs.
 *
 * 📚 EDUCATION:
 * - type: URI to error documentation (e.g., "/errors/AUTH_001")
 * - title: Short description (e.g., "Invalid API Key")
 * - status: HTTP status code
 * - detail: Human-readable explanation
 * - instance: Unique ID for this error occurrence (for support)
 * - errors: Field-level validation errors (optional)
 *
 * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ErrorDetail {

	private String type;              // URI to error docs
	private String title;             // Short description
	private int status;               // HTTP status
	private String detail;            // Human-readable message
	private String instance;          // Unique error ID
	private Instant timestamp;        // When error occurred
	private Map<String, String> errors; // Field validation errors

	/**
	 * Factory method for standard errors
	 */
	public static ErrorDetail of(String errorCode, String title, int status, String detail) {
		return ErrorDetail.builder()
				.type("/errors/" + errorCode)
				.title(title)
				.status(status)
				.detail(detail)
				.instance(UUID.randomUUID().toString())
				.timestamp(Instant.now())
				.build();
	}

	/**
	 * Factory method for validation errors
	 */
	public static ErrorDetail validationError(Map<String, String> fieldErrors) {
		return ErrorDetail.builder()
				.type("/errors/VALIDATION_001")
				.title("Validation Failed")
				.status(400)
				.detail("Request validation failed. Check 'errors' field for details.")
				.instance(UUID.randomUUID().toString())
				.timestamp(Instant.now())
				.errors(fieldErrors)
				.build();
	}
}