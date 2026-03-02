package com.paybridge.webhook.service.provider;

import com.paybridge.common.model.WebhookEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.security.NoSuchAlgorithmException;

@Slf4j
@RequiredArgsConstructor
public class SquadcoSignatureValidator implements SignatureValidator{

	@Value("${providers.squadco.webhook-secret:}")
	private final String secretKey;

	@Override
	public boolean isValid(String rawPayload, String signatureHeader) {
		return false;
	}

	@Override
	public String extractEventId(String rawPayload) throws NoSuchAlgorithmException {
		return "";
	}

	@Override
	public String extractTransactionReference(String rawPayload) {
		return "";
	}

	@Override
	public WebhookEventType normalizeEventType(String rawPayload) {
		return null;
	}
}
