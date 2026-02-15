package com.paybridge.payment.domain.dto;

import java.math.BigDecimal;

public record PaymentResult(
		String reference,
		String status,
		BigDecimal amount) {
}

