package com.paybridge.webhook.consumer;

import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.client.PaymentServiceClient;
import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.dto.PaymentStatusUpdateRequest;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.exception.WebhookProcessingException;
import com.paybridge.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventConsumer {

	private final WebhookEventRepository webhookEventRepository;
	private final PaymentServiceClient   paymentServiceClient;

	/**
	 * Consume normalized webhook events from Kafka.
	 *
	 * Retry and DLQ handling are configured in {@link com.paybridge.webhook.config.KafkaConfig}:
	 * - retryable failures are retried with backoff
	 * - after max attempts (or for permanent failures) the message is published to the
	 *   DLQ and the originating webhook event is marked FAILED
	 */
	@KafkaListener(topics = "webhook-events", groupId = "webhook-processor-group")
	@Transactional
	public void consumeWebhookEvent(@Payload NormalizedWebhookEvent event) {
		try {
			log.info("Processing normalized webhook event: {} | Type: {}",
					event.getEventId(), event.getEventType());

			// Step 1: Find original webhook event
			final UUID originalWebhookId;
			try {
				originalWebhookId = UUID.fromString(event.getOriginalWebhookId());
			} catch (IllegalArgumentException | NullPointerException e) {
				// A malformed ID can never become valid; fail fast to the DLQ instead of retrying
				throw new WebhookProcessingException(
						"Invalid original webhook id: " + event.getOriginalWebhookId(), e);
			}
			WebhookEvent webhookEvent = webhookEventRepository.findById(originalWebhookId)
					.orElseThrow(() -> new WebhookProcessingException(
							"Webhook event not found: " + event.getOriginalWebhookId()));

			// Step 2: Update payment status in payment-service
			PaymentStatusUpdateRequest updateRequest = PaymentStatusUpdateRequest.builder()
					.transactionReference(event.getTransactionReference())
					.newStatus(mapEventTypeToStatus(event.getEventType()))
					.providerResponse(event.getRawPayload())
					.build();

			paymentServiceClient.updatePaymentStatus(updateRequest);
			log.info("Updated payment status for reference: {}", event.getTransactionReference());

			// Step 3: Mark webhook as processed
			webhookEvent.markAsProcessed();
			webhookEventRepository.save(webhookEvent);

			// Offset is committed automatically by the container (AckMode.RECORD)
			log.debug("Processed Kafka message for event {}", event.getEventId());

		} catch (Exception e) {
			log.error("Error processing webhook event {}", event.getEventId(), e);
			// Re-throw so the container's error handler can apply the retry/DLQ policy
			throw e;
		}
	}

	private PaymentStatus mapEventTypeToStatus(WebhookEventType type) {
		return switch (type) {
			case PAYMENT_SUCCESS -> PaymentStatus.SUCCESS;
			case PAYMENT_FAILED, REFUND_FAILED -> PaymentStatus.FAILED;
			case PAYMENT_PENDING -> PaymentStatus.PROCESSING;
			case REFUND_SUCCESS -> PaymentStatus.REFUNDED;
			default -> PaymentStatus.FAILED;
		};
	}
}