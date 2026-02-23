package com.paybridge.webhook.client;

import com.paybridge.webhook.dto.PaymentStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${payment.service.url:http://localhost:8082}")
	private String paymentServiceUrl;

	/**
	 * Update payment status in payment-service
	 * PATCH /internal/payments/status
	 */
	public void updatePaymentStatus(PaymentStatusUpdateRequest request) {
		try {
			WebClient webClient = webClientBuilder.baseUrl(paymentServiceUrl).build();

			Mono<Void> response = webClient.patch()
					.uri("/internal/payments/status")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.retrieve()
					.onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
							clientResponse -> {
								log.error("Payment service returned error: {}", clientResponse.statusCode());
								return Mono.error(new RuntimeException("Failed to update payment status"));
							})
					.bodyToMono(Void.class);

			response.block(); // Blocking call (Kafka consumer is already async)
			log.debug("Payment status updated successfully for reference: {}", request.getTransactionReference());

		} catch (Exception e) {
			log.error("Error calling payment-service to update status", e);
			// Let exception propagate to trigger Kafka retry
			throw new RuntimeException("Payment service communication failed", e);
		}
	}
}