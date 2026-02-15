package com.paybridge.webhook.ingress.application.usecase;

import java.util.Map;

public interface WebhookIngressUseCase {
	void receiveWebhook(String provider, String payload, Map<String, String> headers);
}
