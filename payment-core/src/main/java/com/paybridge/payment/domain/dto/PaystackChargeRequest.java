package com.paybridge.payment.domain.dto;

import java.math.BigDecimal;

public record PaystackChargeRequest(
		BigDecimal amount,
		String currency,
		String customerEmail,
		String description
) {
}
