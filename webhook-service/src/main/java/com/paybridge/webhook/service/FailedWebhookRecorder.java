package com.paybridge.webhook.service;

import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Marks the originating {@link WebhookEvent} as FAILED once a Kafka message has
 * exhausted its retries and is being sent to the DLQ.
 *
 * Runs in a new transaction so the audit trail update succeeds even though the
 * consumer's own transaction was rolled back by the failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedWebhookRecorder {

	private final WebhookEventRepository webhookEventRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Object recordValue, Exception cause) {
		if (!(recordValue instanceof NormalizedWebhookEvent event)) {
			log.warn("Cannot mark webhook as failed: record value is not a NormalizedWebhookEvent");
			return;
		}

		final UUID id;
		try {
			id = UUID.fromString(event.getOriginalWebhookId());
		} catch (IllegalArgumentException | NullPointerException e) {
			log.warn("Cannot mark webhook as failed: invalid original webhook id '{}'",
					event.getOriginalWebhookId());
			return;
		}

		String reason = rootCauseMessage(cause);
		webhookEventRepository.findById(id).ifPresentOrElse(
				webhookEvent -> {
					webhookEvent.markAsFailed(reason);
					webhookEventRepository.save(webhookEvent);
					log.info("Marked webhook event {} as FAILED after retries exhausted", id);
				},
				() -> log.warn("Cannot mark webhook as failed: event {} not found", id));
	}

	private String rootCauseMessage(Throwable throwable) {
		Throwable root = throwable;
		while (root != null && root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		return root != null && root.getMessage() != null ? root.getMessage() : null;
	}
}