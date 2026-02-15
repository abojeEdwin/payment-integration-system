package com.paybridge.webhook.processor.infrastructure.consumer;

import com.paybridge.webhook.processor.application.usecase.WebhookProcessingUseCase;
import com.paybridge.webhook.processor.model.WebhookReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookEventConsumer {

	private final WebhookProcessingUseCase webhookProcessingUseCase;

	@KafkaListener(topics = "webhook-received-events", groupId = "payment-group")
	public void consume(WebhookReceivedEvent event) {
		webhookProcessingUseCase.process(event.webhookId());
	}
}