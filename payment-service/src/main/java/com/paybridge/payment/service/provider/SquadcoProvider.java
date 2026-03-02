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
public class SquadcoProvider implements PaymentProvider {

	private final WebClient webClient;

	@Value("${providers.squadco.secret-key:}")
	private String secretKey;
	@Value("${providers.squadco.api-url:https://api.squadco.co}")
	private String apiUrl;

	public SquadcoProvider(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl(apiUrl).build();
	}

	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		log.info("SquadCoProvider processing payment for {}", request.getCustomerEmail());

		return PaymentResponse.builder()
				.success(true)
				.transactionId("squadco_txn_" + System.currentTimeMillis())
				.reference("SQD_REF_" + System.currentTimeMillis())
				.status(PaymentStatus.SUCCESS)
				.message("Payment initiated successfully")
				.authorizationUrl("https://squadco.com/pay/" + System.currentTimeMillis())
				.build();
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.SQUADCO;
	}
}