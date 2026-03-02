package com.paybridge.payment.service.provider;

import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class PaystackProvider implements PaymentProvider {

	private final WebClient webClient;

	@Value("${paystack.secret-key:}")
	private String secretKey;
	@Value("${paystack.api-url:https://api.paystack.co}")
	private String apiUrl;

	public PaystackProvider(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl(apiUrl).build();
	}

	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		log.info("PaystackProvider processing payment for {}", request.getCustomerEmail());

		// In real implementation: call Paystack API
		// This is a stub that mimics successful response structure
		return PaymentResponse.builder()
				.success(true)
				.transactionId("paystack_txn_" + System.currentTimeMillis())
				.reference("paystack_ref_" + System.currentTimeMillis())
				.status(PaymentStatus.SUCCESS)
				.message("Payment initiated successfully")
				.authorizationUrl("https://paystack.com/pay/" + System.currentTimeMillis())
				.build();
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.PAYSTACK;
	}
}
