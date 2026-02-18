package com.paybridge.webhook.service;

import org.springframework.stereotype.Service;

@Service
public interface WebhookProcessorService {
	 void processWebhookEvent(String eventPayload);
}
