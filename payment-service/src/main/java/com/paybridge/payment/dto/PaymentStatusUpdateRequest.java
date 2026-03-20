package com.paybridge.payment.dto;

import com.paybridge.common.model.PaymentStatus;
import lombok.Data;

@Data
public class PaymentStatusUpdateRequest {
	private String        transactionReference;
	private PaymentStatus newStatus;
	private String        providerResponse;
	private String        provider; // Optional
}