package com.paybridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard wrapper for successful API responses.
 * <p>
 * 📚 EDUCATION:
 * - @JsonInclude(NON_NULL) → omit null fields in JSON
 * - timestamp → helps clients detect stale responses
 * - Builder pattern → fluent construction in controllers
 * - Generic type T → reusable for any response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {
	@Builder.Default
	private boolean success   = true;
	private T       data;
	private String  message;
	@Builder.Default
	private Instant timestamp = Instant.now();

	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder()
				.data(data)
				.build();
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return ApiResponse.<T>builder()
				.data(data)
				.message(message)
				.build();
	}
}