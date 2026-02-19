package com.paybridge.payment.service.provider;

import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.PaymentStatusUpdateRequest;
import com.paybridge.payment.dto.PaymentResponse;

public interface PaymentProvider {
	PaymentResponse charge(PaymentStatusUpdateRequest command);
	ProviderType getProviderType();
}
