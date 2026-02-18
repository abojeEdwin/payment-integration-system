package com.paybridge.payment.service.provider;

import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.ChargeCommand;
import com.paybridge.payment.dto.PaymentResponse;

public class MockProvider implements PaymentProvider{
	@Override
	public PaymentResponse charge(ChargeCommand command) {
		return null;
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.MOCK;
	}
}
