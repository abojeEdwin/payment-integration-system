package com.paybridge.payment.service;

import com.paybridge.payment.dto.PaymentResponse;

public interface PaymentService {
	PaymentResponse processPayment(PaymentRequest request);
}
