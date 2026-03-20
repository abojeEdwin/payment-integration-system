package com.paybridge.auth.config;

import com.paybridge.common.dto.ErrorDetail;
import com.paybridge.common.exception.InvalidApiKeyException;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Handle custom PaymentException
	 */
	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<ErrorDetail> handlePaymentException(PaymentException ex) {
		log.error("Payment exception: {}", ex.getMessage(), ex);

		int status = mapErrorCategoryToHttpStatus(ex.getCategory());

		return ResponseEntity.status(status)
				.body(ErrorDetail.of(
						ex.getErrorCode(),
						ex.getMessage(),
						status,
						ex.getMessage()
				));
	}

	/**
	 * Handle Spring Security BadCredentialsException
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorDetail> handleBadCredentials(BadCredentialsException ex) {
		log.warn("Authentication failed: {}", ex.getMessage());

		return ResponseEntity.status(401)
				.body(ErrorDetail.of(
						"AUTH_003",
						"Invalid credentials",
						401,
						"Email or password is incorrect"
				));
	}

	/**
	 * Handle validation errors from @Valid
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {

		Map<String, String> errors = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.put(error.getField(), error.getDefaultMessage());
		}

		log.warn("Validation failed: {}", errors);

		return ResponseEntity.badRequest()
				.body(ErrorDetail.validationError(errors));
	}

	@ExceptionHandler(InvalidApiKeyException.class)
	public ResponseEntity<ErrorDetail> handleInvalidApiKey(InvalidApiKeyException ex) {
		log.warn("API key error: {}", ex.getMessage());

		return ResponseEntity.status(401)
				.body(ErrorDetail.of(
						ex.getErrorCode(),
						ex.getMessage(),
						401,
						"Provided API key is invalid or inactive"
				));
	}

	/**
	 * Handle invalid authentication principal format (e.g. malformed UUID)
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorDetail> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Invalid argument: {}", ex.getMessage());

		return ResponseEntity.status(400)
				.body(ErrorDetail.of(
						"AUTH_006",
						"Invalid request",
						400,
						ex.getMessage()
				));
	}

	/**
	 * Handle generic exceptions
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDetail> handleGenericException(Exception ex) {
		log.error("Unexpected error: {}", ex.getMessage(), ex);

		return ResponseEntity.status(500)
				.body(ErrorDetail.of(
						"SYS_001",
						"Internal Server Error",
						500,
						"An unexpected error occurred. Please try again later."
				));
	}

	/**
	 * Map ErrorCategory to HTTP status
	 */
	private int mapErrorCategoryToHttpStatus(ErrorCategory category) {
		return switch (category) {
			case CLIENT_ERROR -> 400;
			case BUSINESS_ERROR -> 422;
			case SERVER_ERROR -> 500;
		};
	}
}
