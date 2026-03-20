package com.paybridge.payment.client;

import com.paybridge.payment.dto.MerchantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${auth.service.url:http://localhost:8081}")
	private String authServiceUrl;

	public MerchantInfo validateApiKey(String apiKey) {
		try {
			WebClient webClient = webClientBuilder.baseUrl(authServiceUrl).build();

			return webClient.get()
					.uri("/api-keys/validate")
					.header("X-API-Key", apiKey)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.retrieve()
					.onStatus(
							status -> status.is4xxClientError() || status.is5xxServerError(),
							response -> {
								log.warn("API key validation failed with status: {}", response.statusCode());
								return response.bodyToMono(String.class)
										.map(body -> new RuntimeException("Auth validation failed: " + body));
							})
					.bodyToMono(MerchantInfo.class)
					.block();
		} catch (WebClientResponseException.NotFound e) {
			log.warn("API key not found: {}", maskKey(apiKey));
			return null;
		} catch (Exception e) {
			log.error("Auth service unavailable - failing open for dev", e);
			return null;
		}
	}

	private String maskKey(String key) {
		return key != null && key.length() > 8 ?
				key.substring(0, 4) + "****" + key.substring(key.length() - 4) : "***";
	}


}
