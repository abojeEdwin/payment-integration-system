package com.paybridge.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentEvent {

	private String eventId;
	private String eventType; // PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_FAILED
	private String transactionId;
	private String merchantId;
	private BigDecimal amount;
	private String currency;
	private String status;
	private String provider;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant timestamp;
}