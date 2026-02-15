//For example, the PaystackAdapter would use Spring's WebClient to make
// a POST request to Paystack's transaction initialization endpoint,
// while the InterswitchAdapter might construct a different payload for its
// Quickteller API
// Example Adapter for Paystack in infrastructure/src/main/java/com/paybridge/payment/infrastructure/adapter/gateway/PaystackAdapter.java
package com.paybridge.payment.infrastructure.adapter.gateway;

import com.paybridge.common.model.ChargeCommand;
import com.paybridge.payment.application.port.gateway.PaymentGateway;
import com.paybridge.payment.domain.dto.PaystackResponse;
import com.paybridge.payment.domain.model.*;
import com.paybridge.payment.domain.dto.PaymentResult;
import com.paybridge.payment.domain.dto.PaystackChargeRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Qualifier("paystack")
public class PaystackAdapter implements PaymentGateway {

	private final WebClient          webClient;
	private final PaystackProperties config;

	public PaystackAdapter(WebClient.Builder webClientBuilder, PaystackProperties config) {
		this.webClient = webClientBuilder.build();
		this.config = config;
	}

	@Override
	public PaymentResult charge(ChargeCommand command) {
		// 1. Map domain command to Paystack-specific request DTO
		PaystackChargeRequest request = new PaystackChargeRequest(
				command.amount(),
				command.currency(),
				command.customerEmail(),
				command.description()
		);

		// 2. Make the external API call
		PaystackResponse response = webClient.post()
				.uri(config.getApiUrl() + "/transaction/initialize")
				.header("Authorization", "Bearer " + config.getSecretKey())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(PaystackResponse.class)
				.block();

		// 3. Map the external response to the domain result
		assert response != null; // Handle null response appropriately in production code
		return new PaymentResult(
				response.reference(),
				"SUCCESS".equals(response.status()) ? "SUCCESS" : "FAILED",
				command.amount());
	}

	@Override
	public PaymentResult refund(String transactionId, Double amount) {
		return null;
	}
	// ... Refund implementation
}