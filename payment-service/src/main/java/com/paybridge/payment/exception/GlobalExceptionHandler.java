package com.paybridge.payment.exception;

import com.paybridge.common.dto.ErrorDetail;
import com.paybridge.common.exception.PaymentException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<ErrorDetail> handlePayment(PaymentException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(new ErrorDetail(
						ex.getStatus().value(),
						ex.getMessage()));
	}
}
