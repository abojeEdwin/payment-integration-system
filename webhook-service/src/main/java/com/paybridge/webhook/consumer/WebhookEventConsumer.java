package com.paybridge.webhook.consumer;

import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.client.PaymentServiceClient;
import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.dto.PaymentStatusUpdateRequest;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventConsumer {

	private final WebhookEventRepository webhookEventRepository;
	private final PaymentServiceClient   paymentServiceClient;

	/**
	 * Consume normalized webhook events from Kafka
	 * Manual acknowledgment for reliability
	 */
	@KafkaListener(topics = "webhook-events", groupId = "webhook-processor-group")
	@Transactional
	public void consumeWebhookEvent(@Payload NormalizedWebhookEvent event, Acknowledgment ack) {
		try {
			log.info("Processing normalized webhook event: {} | Type: {}",
					event.getEventId(), event.getEventType());

			// Step 1: Find original webhook event
			WebhookEvent webhookEvent = webhookEventRepository.findById(
							java.util.UUID.fromString(event.getOriginalWebhookId()))
					.orElseThrow(() -> new RuntimeException("Webhook event not found: " + event.getOriginalWebhookId()));

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

			// Step 4: Acknowledge Kafka message (commit offset)
			ack.acknowledge();
			log.debug("Acknowledged Kafka message for event {}", event.getEventId());

		} catch (Exception e) {
			log.error("Error processing webhook event {}", event.getEventId(), e);
			// Do NOT acknowledge - message will be reprocessed after restart
			// In production: Add retry logic + DLQ after max attempts
			throw e; // Re-throw to trigger retry
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
