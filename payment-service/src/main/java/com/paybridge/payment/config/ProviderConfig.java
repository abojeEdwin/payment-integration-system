package com.paybridge.payment.config;

import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.service.provider.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
public class ProviderConfig {

	@Bean
	public ProviderFactory providerFactory(
			MockProvider mockProvider,
			PaystackProvider paystackProvider,
			InterswitchProvider interswitchProvider,
			SquadcoProvider squadCoProvider) {

		Map<ProviderType, PaymentProvider> providers = Map.of(
				ProviderType.PAYSTACK, paystackProvider,
				ProviderType.INTERSWITCH, interswitchProvider,
				ProviderType.SQUADCO, squadCoProvider,
				ProviderType.MOCK, mockProvider
		);

		return new ProviderFactory(providers);
	}

	/**
	 * Default provider for testing (can be overridden via config)
	 */
	@Bean
	public PaymentProvider defaultProvider(ProviderFactory factory) {
		return factory.getProvider(ProviderType.MOCK); // Start with mock for safety
	}


}