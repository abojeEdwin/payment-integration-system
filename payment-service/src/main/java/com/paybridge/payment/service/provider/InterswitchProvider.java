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
public class InterswitchProvider implements PaymentProvider {
	private final WebClient webClient;
	private final String secretKey;

	public InterswitchProvider(
			WebClient.Builder webClientBuilder,
			@Value("${providers.interswitch.api-url:https://api.interswitch.co}") String apiUrl,
			@Value("${providers.interswitch.secret-key:}") String secretKey) {
		this.webClient = webClientBuilder.baseUrl(apiUrl).build();
		this.secretKey = secretKey;
	}

	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		log.info("InterswitchProvider processing payment for {}", request.getCustomerEmail());

		return PaymentResponse.builder()
				.success(true)
				.transactionId("interswitch_txn_" + System.currentTimeMillis())
				.reference("ISW_REF_" + System.currentTimeMillis())
				.status(PaymentStatus.SUCCESS)
				.message("Payment initiated successfully")
				.authorizationUrl("https://interswitch.com/pay/" + System.currentTimeMillis())
				.build();
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.INTERSWITCH;
	}
}
