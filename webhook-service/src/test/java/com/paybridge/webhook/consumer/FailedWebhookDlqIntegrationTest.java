package com.paybridge.webhook.consumer;

import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.client.PaymentServiceClient;
import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.dto.PaymentStatusUpdateRequest;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * End-to-end verification of the DLQ behaviour: a message that fails processing is
 * retried (config) and, once attempts are exhausted, published to the DLQ topic while
 * the originating {@link WebhookEvent} is marked FAILED.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(topics = {"webhook-events", "webhook-events.DLT"}, partitions = 1)
@ActiveProfiles("test")
@DirtiesContext
class FailedWebhookDlqIntegrationTest {

	@Autowired
	private WebhookEventRepository webhookEventRepository;

	@Autowired
	private KafkaTemplate<String, NormalizedWebhookEvent> kafkaTemplate;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@MockBean
	private PaymentServiceClient paymentServiceClient;

	@Test
	void failedMessage_isPublishedToDlqAndWebhookMarkedFailed() {
		doThrow(new RuntimeException("payment-service down"))
				.when(paymentServiceClient).updatePaymentStatus(any(PaymentStatusUpdateRequest.class));

		WebhookEvent saved = webhookEventRepository.save(WebhookEvent.builder()
				.provider("paystack")
				.eventType(WebhookEventType.PAYMENT_SUCCESS)
				.rawPayload("{}")
				.status("PROCESSING")
				.build());

		NormalizedWebhookEvent event = NormalizedWebhookEvent.builder()
				.eventId(UUID.randomUUID().toString())
				.originalWebhookId(saved.getId().toString())
				.provider("paystack")
				.eventType(WebhookEventType.PAYMENT_SUCCESS)
				.transactionReference("txn_123")
				.rawPayload("{}")
				.receivedAt(Instant.now())
				.build();

		kafkaTemplate.send("webhook-events", event.getEventId(), event);
		kafkaTemplate.flush();

		NormalizedWebhookEvent dlqEvent;
		try (org.apache.kafka.clients.consumer.Consumer<String, NormalizedWebhookEvent> consumer =
				dlqConsumerFactory().createConsumer()) {
			consumer.assign(java.util.Collections.singletonList(
					new org.apache.kafka.common.TopicPartition("webhook-events.DLT", 0)));
			dlqEvent = KafkaTestUtils.getSingleRecord(consumer, "webhook-events.DLT").value();
		}

		assertEquals(event.getEventId(), dlqEvent.getEventId());

		WebhookEvent stored = webhookEventRepository.findById(saved.getId()).orElseThrow();
		assertEquals("FAILED", stored.getStatus());
		assertEquals("payment-service down", stored.getFailureReason());
	}

	private DefaultKafkaConsumerFactory<String, NormalizedWebhookEvent> dlqConsumerFactory() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
				"dlq-test-group", "false", embeddedKafkaBroker);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		return new DefaultKafkaConsumerFactory<>(props,
				new StringDeserializer(), new JsonDeserializer<>(NormalizedWebhookEvent.class));
	}
}