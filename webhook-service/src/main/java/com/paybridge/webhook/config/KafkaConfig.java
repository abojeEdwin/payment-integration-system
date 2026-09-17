package com.paybridge.webhook.config;

import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.exception.WebhookProcessingException;
import com.paybridge.webhook.service.FailedWebhookRecorder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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
				kafkaTemplate, (record, exception) -> new TopicPartition(dlqTopic, record.partition()));

		ConsumerRecordRecoverer recoverer = (record, exception) -> {
			failedWebhookRecorder.markFailed(record.value(), exception);
			log.warn("Webhook event {} exhausted retries; publishing to DLQ {}",
					record.key(), dlqTopic, exception);
			deadLetterRecoverer.accept(record, exception);
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