package com.paybridge.payment.dto;

import com.paybridge.common.model.Currency;
import com.paybridge.common.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentResponse {

	private boolean success;
	private String transactionId;
	private String reference; // Provider's reference
	private PaymentStatus status;
	private BigDecimal amount;
	private Currency currency;
	private String customerEmail;
	private String message;
	private String authorizationUrl; // For redirect-based payments
}