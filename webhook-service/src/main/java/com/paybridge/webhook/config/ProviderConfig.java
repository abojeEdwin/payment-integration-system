package com.paybridge.webhook.config;

import com.paybridge.webhook.service.provider.InterswitchSignatureValidator;
import com.paybridge.webhook.service.provider.PaystackSignatureValidator;
import com.paybridge.webhook.service.provider.SignatureValidator;
import com.paybridge.webhook.service.provider.SquadcoSignatureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ProviderConfig {

	@Value("${providers.paystack.webhook-secret:}")
	private String paystackSecret;

	@Value("${providers.interswitch.webhook-secret:}")
	private String interswitchSecret;

	@Value("${providers.squadco.webhook-secret:}")
	private String squadcoSecret;

	@Bean
	public SignatureValidator paystackValidator() {
		return new PaystackSignatureValidator(paystackSecret);
	}

	@Bean
	public SignatureValidator interswitchValidator() {
		return new InterswitchSignatureValidator(interswitchSecret);
	}

	@Bean
	public SignatureValidator squadcoValidator() {
		return new SquadcoSignatureValidator(squadcoSecret);
	}

	@Bean
	public Map<String, SignatureValidator> signatureValidators(
			SignatureValidator paystackValidator,
			SignatureValidator interswitchValidator,
			SignatureValidator squadcoValidator) {

		return Map.of(
				"paystack", paystackValidator,
				"interswitch", interswitchValidator,
				"squadco", squadcoValidator
		);
	}
}