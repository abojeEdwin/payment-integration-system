package com.paybridge.webhook.service;

import com.paybridge.webhook.dto.WebhookPayload;

public class KafkaEventIngressService implements WebhookIngressService {

	@Override
	public void saveAndPublish(String provider, String rawPayload) {

	}

	@Override
	public void saveAndPublish(WebhookPayload webhookPayload) {

	}
}