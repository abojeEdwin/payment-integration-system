package com.paybridge.payment.service.provider;

import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;

public interface PaymentProvider {
	PaymentResponse charge(CreatePaymentRequest request);
	ProviderType getProviderType();
}
