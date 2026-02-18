package com.paybridge.webhook.service;

import com.paybridge.webhook.dto.WebhookPayload;
import org.springframework.stereotype.Service;

@Service
public interface WebhookIngressService {
	void saveAndPublish(String provider, String rawPayload);

	void saveAndPublish(WebhookPayload webhookPayload);
}