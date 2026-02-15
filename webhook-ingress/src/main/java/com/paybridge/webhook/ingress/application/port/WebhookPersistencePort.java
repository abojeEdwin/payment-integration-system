package com.paybridge.webhook.ingress.application.port;

public interface WebhookPersistencePort {
	void saveRawWebhook(String provider, String eventType,
						String payload, String signatureHeader);
}
