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
public class SquadcoProvider implements PaymentProvider {

	private final WebClient webClient;
	private final String    secretKey;
	private final String    webhookUrl;
	private final String    SQUADCO_CHARGE_PATH = "/transaction/initiate";

	public SquadcoProvider(
			WebClient.Builder webClientBuilder,
			@Value("${providers.squadco.secret-key}") String secretKey,
			@Value("${providers.squadco.api-url}") String apiUrl,
			@Value("${webhook.service.url}") String webhookUrl) {
		this.webClient = webClientBuilder
				.baseUrl(apiUrl)
				.defaultHeader("Authorization", "Bearer " + secretKey)
				.defaultHeader("Content-Type", "application/json")
				.build();
		this.secretKey = secretKey;
		this.webhookUrl = webhookUrl + "/squadco"; // Construct callback URL
	}

	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		try {
			SquadcoChargeRequest squadcoRequest = SquadcoChargeRequest.builder()
					.email(request.getCustomerEmail())
					.amount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to kobo
					.currency(request.getCurrency().toString())
					.transaction_ref(generateReference(request.getIdempotencyKey()))
					.callback_url(webhookUrl)
					.metadata(Map.of("custom_fields", List.of(
							Map.of("display_name", "Payment ID",
									"variable_name", "payment_id",
									"value", request.getIdempotencyKey())
					)))
					.build();

			SquadcoChargeResponse response = webClient.post()
					.uri(SQUADCO_CHARGE_PATH)
					.bodyValue(squadcoRequest)
					.retrieve()
					.bodyToMono(SquadcoChargeResponse.class)
					.block();

			if (response == null || !response.isSuccess()) {
				throw new RuntimeException("Squadco charge failed: " +
						(response != null ? response.getMessage() : "No response"));
			}

			return PaymentResponse.builder()
					.success(true)
					.transactionId(response.getData().getId().toString())
					.reference(squadcoRequest.getTransaction_ref())
					.status(PaymentStatus.SUCCESS)
					.message("Payment initiated successfully")
					.authorizationUrl(response.getData().getCheckout_url())
					.build();

		} catch (Exception e) {
			log.error("Squadco API error", e);
			throw new ProviderCommunicationException(
					"SQUADCO", "Charge Failed", e);
		}
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.SQUADCO;
	}

	private String generateReference(String idempotencyKey) {
		return "pay_" + idempotencyKey.substring(6, 20); // Extract timestamp portion
	}

	// DTOs for Paystack API
	@Data
	@Builder
	private static class SquadcoChargeRequest {
		private String              email;
		private Long                amount;
		@Builder.Default
		private String              initiate_type = "inline";
		private String              currency;
		private String              transaction_ref;
		private String              callback_url;
		@Builder.Default
		private boolean             pass_charge   = true;
		private Map<String, Object> metadata;
	}

	@Data
	private static class SquadcoChargeResponse {
		private int               status;
		private String            message;
		private SquadcoChargeData data;

		public boolean isSuccess() {
			return status == 200;
		}
	}

	@Data
	private static class SquadcoChargeData {
		private Long   id;
		private String transaction_ref;
		private String checkout_url;
	}
}