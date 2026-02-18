package com.paybridge.payment.config;

import com.paybridge.common.dto.ErrorDetail;
import com.paybridge.common.exception.CommonException;
import com.paybridge.common.model.ErrorCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CommonException.class)
	public ResponseEntity<ErrorDetail> handlePayment(CommonException ex) {
		int httpStatus = mapToHttpStatus(ex.getCategory());
		return ResponseEntity.status(httpStatus)
				.body(ErrorDetail.of(
						ex.getErrorCode(),
						ex.getMessage(),
						httpStatus,
						ex.getMessage()));
	}

	private int mapToHttpStatus(ErrorCategory category) {
		return switch (category) {
			case CLIENT_ERROR -> 400;
			case BUSINESS_ERROR -> 422; // Unprocessable Entity for business rules
			case SERVER_ERROR -> 502;   // Bad Gateway for provider failures
		};
	}
}