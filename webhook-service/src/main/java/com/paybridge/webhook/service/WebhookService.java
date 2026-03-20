package com.paybridge.webhook.service;

import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.entity.ProcessedWebhook;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.exception.InvalidSignatureException;
import com.paybridge.webhook.repository.ProcessedWebhookRepository;
import com.paybridge.webhook.repository.WebhookEventRepository;
import com.paybridge.webhook.service.provider.SignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

	private final WebhookEventRepository                        webhookEventRepository;
	private final ProcessedWebhookRepository                    processedWebhookRepository;
	private final Map<String, SignatureValidator>               signatureValidators;
	private final KafkaTemplate<String, NormalizedWebhookEvent> kafkaTemplate;

	private static final String TOPIC_WEBHOOK_EVENTS = "webhook-events";

	/**
	 * Process webhook after persistence
	 * 1. Validate signature
	 * 2. Check idempotency
	 * 3. Normalize event
	 * 4. Publish to Kafka
	 */
	@Transactional
	public void processWebhook(WebhookEvent webhookEvent) {
		try {
			// Step 1: Get validator for provider
			SignatureValidator validator = signatureValidators.get(webhookEvent.getProvider().toLowerCase());
			if (validator == null) {
				String errorMsg = "No signature validator for provider: " + webhookEvent.getProvider();
				webhookEvent.markAsFailed(errorMsg);
				webhookEventRepository.save(webhookEvent);
				throw new IllegalArgumentException(errorMsg);
			}

			final String rawPayload = webhookEvent.getRawPayload();
			final String signatureHeader = webhookEvent.getSignatureHeader();

			// Step 2: Validate signature
			if (!validator.isValid(rawPayload, signatureHeader)) {
				log.warn("Invalid signature for webhook {}", webhookEvent.getId());
				webhookEvent.markAsInvalidSignature();
				webhookEventRepository.save(webhookEvent);
				throw new InvalidSignatureException("Invalid webhook signature");
			}

			// Step 3: Check idempotency
			String eventId = validator.extractEventId(rawPayload);
			if (processedWebhookRepository.existsByProviderAndEventId(webhookEvent.getProvider(), eventId)) {
				log.info("Duplicate webhook detected (idempotency): {}", eventId);
				webhookEvent.markAsProcessed(); // Mark as processed to avoid reprocessing
				webhookEventRepository.save(webhookEvent);
				return; // Skip processing
			}

			// Step 4: Normalize event
			WebhookEventType eventType = validator.normalizeEventType(rawPayload);
			String transactionRef = validator.extractTransactionReference(rawPayload);

			NormalizedWebhookEvent normalizedEvent = NormalizedWebhookEvent.builder()
					.eventId(UUID.randomUUID().toString())
					.originalWebhookId(webhookEvent.getId().toString())
					.provider(webhookEvent.getProvider())
					.eventType(eventType)
					.transactionReference(transactionRef)
					.rawPayload(rawPayload)
					.receivedAt(webhookEvent.getReceivedAt())
					.build();

			// Step 5: Publish to Kafka
			kafkaTemplate.send(TOPIC_WEBHOOK_EVENTS, normalizedEvent.getEventId(), normalizedEvent);
			log.info("Published normalized event {} to Kafka", normalizedEvent.getEventId());

			// Step 6: Mark webhook as processing
			webhookEvent.setStatus("PROCESSING");
			webhookEventRepository.save(webhookEvent);

			// Step 7: Record processed webhook for idempotency
			ProcessedWebhook processed = ProcessedWebhook.builder()
					.provider(webhookEvent.getProvider())
					.eventId(eventId)
					.build();
			processedWebhookRepository.save(processed);

		} catch (Exception e) {
			log.error("Error processing webhook {}", webhookEvent.getId(), e);
			webhookEvent.markAsFailed(e.getMessage());
			webhookEventRepository.save(webhookEvent);
		}
	}
}
