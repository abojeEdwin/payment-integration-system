package com.paybridge.webhook.config;

import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.exception.WebhookProcessingException;
import com.paybridge.webhook.service.FailedWebhookRecorder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${webhook.kafka.dlq-topic:webhook-events.DLT}")
	private String dlqTopic;

	@Value("${webhook.kafka.retry.max-attempts:3}")
	private int maxAttempts;

	@Value("${webhook.kafka.retry.backoff-interval-ms:1000}")
	private long backoffIntervalMs;

	// Producer config for publishing normalized events
	@Bean
	public ProducerFactory<String, NormalizedWebhookEvent> producerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		props.put(ProducerConfig.ACKS_CONFIG, "all"); // Strong durability
		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	public KafkaTemplate<String, NormalizedWebhookEvent> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	// Consumer config for processing events
	@Bean
	public ConsumerFactory<String, NormalizedWebhookEvent> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "webhook-processor-group");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		return new DefaultKafkaConsumerFactory<>(props);
	}

	/**
	 * Error handler that retries messages with backoff and, after the attempts are
	 * exhausted (or for permanent failures), publishes them to the DLQ and marks the
	 * originating {@link com.paybridge.webhook.entity.WebhookEvent} as FAILED.
	 */
	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, NormalizedWebhookEvent> kafkaTemplate,
	                                             FailedWebhookRecorder failedWebhookRecorder) {
		DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate, (record, exception) -> {
					// Preserve per-partition ordering while tolerating a DLQ with fewer partitions
					// than the source topic: mod-mapping maps each source partition to one DLQ
					// partition, keeping events ordered per partition. If the DLQ is unknown, fall
					// back to partition 0 and let the send fail (below) rather than committing.
					List<PartitionInfo> dlqPartitions = kafkaTemplate.partitionsFor(dlqTopic);
					if (dlqPartitions == null || dlqPartitions.isEmpty()) {
						return new TopicPartition(dlqTopic, 0);
					}
					return new TopicPartition(dlqTopic, Math.floorMod(record.partition(), dlqPartitions.size()));
				});
		// Throw on DLQ send failure so the source offset is not committed (setCommitRecovered(true))
		// until the event has actually been published to the DLQ. Otherwise an async send that
		// fails (unavailable topic, bad partition, serialization error) would silently drop the event.
		deadLetterRecoverer.setFailIfSendResultIsError(true);

		ConsumerRecordRecoverer recoverer = (record, exception) -> {
			log.warn("Webhook event {} exhausted retries; publishing to DLQ {}",
					record.key(), dlqTopic, exception);
			// Publish to the DLQ first: if it fails, the recoverer throws and the source offset
			// is not committed, so the FAILED status below reflects a confirmed DLQ copy.
			deadLetterRecoverer.accept(record, exception);
			failedWebhookRecorder.markFailed(record.value(), exception);
		};

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
				new FixedBackOff(backoffIntervalMs, maxAttempts - 1L));
		// Permanent failures go straight to the DLQ instead of burning through retries
		errorHandler.addNotRetryableExceptions(WebhookProcessingException.class);
		errorHandler.setCommitRecovered(true);
		return errorHandler;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, NormalizedWebhookEvent> kafkaListenerContainerFactory(
			DefaultErrorHandler kafkaErrorHandler) {
		ConcurrentKafkaListenerContainerFactory<String, NormalizedWebhookEvent> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
		factory.setCommonErrorHandler(kafkaErrorHandler);
		return factory;
	}
}