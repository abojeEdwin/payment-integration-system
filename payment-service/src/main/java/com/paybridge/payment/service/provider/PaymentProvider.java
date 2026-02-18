package com.paybridge.payment.service.provider;

import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.ChargeCommand;
import com.paybridge.payment.dto.PaymentResponse;

public interface PaymentProvider {
	PaymentResponse charge(ChargeCommand command);
	ProviderType getProviderType();
}
