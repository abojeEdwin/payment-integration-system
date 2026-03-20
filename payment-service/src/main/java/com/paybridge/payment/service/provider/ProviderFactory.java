package com.paybridge.payment.service.provider;

import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import com.paybridge.common.model.ProviderType;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ProviderFactory {
	private final Map<ProviderType, PaymentProvider> providers;

	public PaymentProvider getProvider(ProviderType type) {
		PaymentProvider provider = providers.get(type);
		if (provider == null) {
			throw new PaymentException(
					"Unsupported provider: " + type,
					"PAY_001",
					ErrorCategory.CLIENT_ERROR
			);
		}
		return provider;
	}

}
