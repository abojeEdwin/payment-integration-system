package com.paybridge.webhook.service;

import com.paybridge.webhook.dto.NormalizedWebhookEvent;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailedWebhookRecorderTest {

	@Mock
	private WebhookEventRepository webhookEventRepository;

	@InjectMocks
	private FailedWebhookRecorder recorder;

	@Test
	void markFailed_marksEventAsFailedWhenFound() {
		UUID id = UUID.randomUUID();
		WebhookEvent event = WebhookEvent.builder().id(id).status("PROCESSING").build();
		when(webhookEventRepository.findById(id)).thenReturn(Optional.of(event));

		recorder.markFailed(NormalizedWebhookEvent.builder()
				.originalWebhookId(id.toString())
				.build(), new RuntimeException("boom"));

		assertEquals("FAILED", event.getStatus());
		assertEquals("boom", event.getFailureReason());
		verify(webhookEventRepository).save(event);
	}

	@Test
	void markFailed_ignoresNonWebhookValues() {
		recorder.markFailed("not a webhook event", new RuntimeException("boom"));

		verifyNoInteractions(webhookEventRepository);
	}

	@Test
	void markFailed_ignoresWhenEventNotFound() {
		UUID id = UUID.randomUUID();
		when(webhookEventRepository.findById(id)).thenReturn(Optional.empty());

		recorder.markFailed(NormalizedWebhookEvent.builder()
				.originalWebhookId(id.toString())
				.build(), new RuntimeException("boom"));

		verify(webhookEventRepository, never()).save(any());
	}

	@Test
	void markFailed_ignoresInvalidWebhookId() {
		recorder.markFailed(NormalizedWebhookEvent.builder()
				.originalWebhookId("not-a-uuid")
				.build(), new RuntimeException("boom"));

		verifyNoInteractions(webhookEventRepository);
	}
}