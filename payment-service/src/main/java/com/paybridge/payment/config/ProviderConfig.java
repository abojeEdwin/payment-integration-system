package com.paybridge.payment.config;

import com.paybridge.payment.service.provider.MockProvider;
import com.paybridge.payment.service.provider.PaymentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ProviderConfig {

	@Bean
	@Primary
	public PaymentProvider mockProvider() {
		return new MockProvider();
	}

	// Uncomment when ready for real providers:
//	@Bean
//	public PaymentProvider paystackProvider() {
//		return new PaystackProvider();
//	}
}