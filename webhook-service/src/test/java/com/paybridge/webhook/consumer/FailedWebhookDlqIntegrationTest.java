package com.paybridge.webhook.consumer;

import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.client.PaymentServiceClient;
import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.dto.PaymentStatusUpdateRequest;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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

		NormalizedWebhookEvent dlqEvent = waitForDlqEvent(event.getEventId());

		assertEquals(event.getEventId(), dlqEvent.getEventId());

		// The recoverer publishes to the DLQ before markFailed() commits (REQUIRES_NEW), so the DLQ
		// record can arrive before the DB status; await the FAILED status before asserting details.
		Awaitility.await().atMost(Duration.ofSeconds(15))
				.until(() -> webhookEventRepository.findById(saved.getId())
						.map(WebhookEvent::getStatus)
						.filter("FAILED"::equals)
						.isPresent());
		WebhookEvent stored = webhookEventRepository.findById(saved.getId()).orElseThrow();
		assertEquals("FAILED", stored.getStatus());
		assertEquals("payment-service down", stored.getFailureReason());
	}

	@Test
	void malformedPayload_isPublishedToDlqWithOriginalBytes() throws Exception {
		byte[] malformed = "not valid json".getBytes(StandardCharsets.UTF_8);

		Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
		try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps)) {
			producer.send(new ProducerRecord<>("webhook-events", "bad-key", malformed))
					.get(10, TimeUnit.SECONDS);
		}

		byte[] dlqPayload = waitForDlqBytes(malformed);
		assertArrayEquals(malformed, dlqPayload);
	}

	@Test
	void malformedOriginalWebhookId_isPermanentFailure_andPublishedToDlq() {
		NormalizedWebhookEvent event = NormalizedWebhookEvent.builder()
				.eventId(UUID.randomUUID().toString())
				.originalWebhookId("not-a-uuid")
				.provider("paystack")
				.eventType(WebhookEventType.PAYMENT_SUCCESS)
				.transactionReference("txn_malformed")
				.rawPayload("{}")
				.receivedAt(Instant.now())
				.build();

		kafkaTemplate.send("webhook-events", event.getEventId(), event);
		kafkaTemplate.flush();

		NormalizedWebhookEvent dlqEvent = waitForDlqEvent(event.getEventId());

		assertEquals(event.getEventId(), dlqEvent.getEventId());
	}

	/**
	 * Polls the DLQ until an event matching the expected id arrives. Reads by id (instead of
	 * {@link KafkaTestUtils#getSingleRecord}) because the embedded broker is shared across test
	 * methods, so more than one record may already be present in the DLQ.
	 */
	private NormalizedWebhookEvent waitForDlqEvent(String expectedEventId) {
		try (org.apache.kafka.clients.consumer.Consumer<String, NormalizedWebhookEvent> consumer =
				dlqConsumerFactory().createConsumer()) {
			consumer.assign(java.util.Collections.singletonList(
					new org.apache.kafka.common.TopicPartition("webhook-events.DLT", 0)));
			long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
			while (System.currentTimeMillis() < deadline) {
				ConsumerRecords<String, NormalizedWebhookEvent> records =
						KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1));
				for (ConsumerRecord<String, NormalizedWebhookEvent> record : records) {
					if (expectedEventId.equals(record.value().getEventId())) {
						return record.value();
					}
				}
			}
		}
		fail("DLQ event " + expectedEventId + " not received");
		return null;
	}

	private DefaultKafkaConsumerFactory<String, NormalizedWebhookEvent> dlqConsumerFactory() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
				"dlq-test-group", "false", embeddedKafkaBroker);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		return new DefaultKafkaConsumerFactory<>(props,
				new StringDeserializer(), new JsonDeserializer<>(NormalizedWebhookEvent.class));
	}

	/**
	 * Polls the DLQ for the raw byte[] payload of a record whose JSON deserialization failed,
	 * matching the exact bytes (other test methods may have left well-formed records in the DLQ).
	 */
	private byte[] waitForDlqBytes(byte[] expected) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
				"dlq-bytes-test-group", "false", embeddedKafkaBroker);
		try (org.apache.kafka.clients.consumer.Consumer<String, byte[]> consumer =
				new DefaultKafkaConsumerFactory<>(props,
						new StringDeserializer(), new ByteArrayDeserializer()).createConsumer()) {
			consumer.assign(java.util.Collections.singletonList(
					new org.apache.kafka.common.TopicPartition("webhook-events.DLT", 0)));
			long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
			while (System.currentTimeMillis() < deadline) {
				ConsumerRecords<String, byte[]> records =
						KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1));
				for (ConsumerRecord<String, byte[]> record : records) {
					if (Arrays.equals(expected, record.value())) {
						return record.value();
					}
				}
			}
		}
		fail("DLQ payload not received for malformed record");
		return null;
	}
}