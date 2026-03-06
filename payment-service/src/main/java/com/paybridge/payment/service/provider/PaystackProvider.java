package com.paybridge.payment.service.provider;

import com.paybridge.common.exception.ProviderCommunicationException;
import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PaystackProvider implements PaymentProvider {

	private final WebClient webClient;
	private final String    secretKey;
	private final String    webhookUrl;
	private final String    PAYSTACK_CHARGE_PATH = "/transaction/initialize";

	public PaystackProvider(
			WebClient.Builder webClientBuilder,
			@Value("${providers.paystack.secret-key}") String secretKey,
			@Value("${providers.paystack.api-url}") String apiUrl,
			@Value("${webhook.service.url}") String webhookUrl) {
		this.webClient = webClientBuilder
				.baseUrl(apiUrl)
				.defaultHeader("Authorization", "Bearer " + secretKey)
				.defaultHeader("Content-Type", "application/json")
				.build();
		this.secretKey = secretKey;
		this.webhookUrl = webhookUrl + "/paystack"; // Construct callback URL
	}

	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		try {
			PaystackChargeRequest paystackRequest = PaystackChargeRequest.builder()
					.email(request.getCustomerEmail())
					.amount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to kobo
					.currency(request.getCurrency().toString())
					.reference(generateReference(request.getIdempotencyKey()))
					.callback_url(webhookUrl)
					.metadata(Map.of("custom_fields", List.of(
							Map.of("display_name", "Payment ID",
									"variable_name", "payment_id",
									"value", request.getIdempotencyKey())
					)))
					.build();

			PaystackChargeResponse response = webClient.post()
					.uri(PAYSTACK_CHARGE_PATH)
					.bodyValue(paystackRequest)
					.retrieve()
					.bodyToMono(PaystackChargeResponse.class)
					.block();

			if (response == null || !response.isStatus()) {
				throw new RuntimeException("Paystack charge failed: " +
						(response != null ? response.getMessage() : "No response"));
			}

			return PaymentResponse.builder()
					.success(true)
					.transactionId(response.getData().getId().toString())
					.reference(paystackRequest.getReference())
					.status(PaymentStatus.SUCCESS)
					.message("Payment initiated successfully")
					.authorizationUrl(response.getData().getAuthorizationUrl())
					.build();

		} catch (Exception e) {
			log.error("Paystack API error", e);
			throw new ProviderCommunicationException(
					"PAYSTACK", "Charge Failed", e);
		}
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.PAYSTACK;
	}

	private String generateReference(String idempotencyKey) {
		return "pay_" + idempotencyKey.substring(6, 20); // Extract timestamp portion
	}

	// DTOs for Paystack API
	@Data
	@Builder
	private static class PaystackChargeRequest {
		private String              email;
		private Long                amount;
		private String              currency;
		private String              reference;
		private String              callback_url;
		private Map<String, Object> metadata;
	}

	@Data
	private static class PaystackChargeResponse {
		private boolean            status;
		private String             message;
		private PaystackChargeData data;
	}

	@Data
	private static class PaystackChargeData {
		private Long   id;//use access_code instead of id (similar to paystack)
		private String reference;
		private String authorizationUrl;
	}
}
