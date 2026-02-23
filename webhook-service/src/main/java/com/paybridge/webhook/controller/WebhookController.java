package com.paybridge.webhook.controller;

import com.paybridge.common.model.WebhookEventType;
import com.paybridge.webhook.entity.WebhookEvent;
import com.paybridge.webhook.repository.WebhookEventRepository;
import com.paybridge.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PUBLIC endpoints - called by payment providers
 * NO authentication required (security via signature validation)
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

	private final WebhookEventRepository webhookEventRepository;
	private final WebhookService         webhookService;

	/**
	 * Paystack webhook endpoint
	 * POST /webhooks/paystack
	 * Headers: x-paystack-signature
	 */
	@PostMapping("/paystack")
	public ResponseEntity<Void> handlePaystackWebhook(
			@RequestHeader(value = "x-paystack-signature", required = false) String signature,
			@RequestBody String rawPayload) {

		return handleWebhook("paystack", signature, rawPayload);
	}

	/**
	 * Interswitch webhook endpoint
	 * POST /webhooks/interswitch
	 */
	@PostMapping("/interswitch")
	public ResponseEntity<Void> handleInterswitchWebhook(
			@RequestHeader(value = "Authorization", required = false) String signature,
			@RequestBody String rawPayload) {

		return handleWebhook("interswitch", signature, rawPayload);
	}

	/**
	 * SquadCo webhook endpoint
	 * POST /webhooks/squadco
	 */
	@PostMapping("/squadco")
	public ResponseEntity<Void> handleSquadcoWebhook(
			@RequestBody String rawPayload) {

		// SquadCo may not send signature in header (depends on implementation)
		return handleWebhook("squadco", null, rawPayload);
	}

	/**
	 * Generic webhook handler
	 * CRITICAL: Persist raw payload BEFORE any processing
	 */
	private ResponseEntity<Void> handleWebhook(String provider, String signature, String rawPayload) {
		try {
			log.info("Received webhook from {} | Payload size: {} bytes", provider, rawPayload.length());

			// ✅ STEP 1: PERSIST RAW PAYLOAD IMMEDIATELY (audit trail guaranteed)
			WebhookEvent webhookEvent = WebhookEvent.builder()
					.provider(provider)
					.eventType(WebhookEventType.UNKNOWN) // Will be normalized later
					.rawPayload(rawPayload)
					.signatureHeader(signature)
					.status("PENDING")
					.build();

			webhookEvent = webhookEventRepository.save(webhookEvent);
			log.debug("Persisted webhook event: {}", webhookEvent.getId());

			// ✅ STEP 2: Delegate to service for async processing
			webhookService.processWebhook(webhookEvent);

			// ✅ STEP 3: Acknowledge receipt IMMEDIATELY (required by providers)
			return ResponseEntity.ok().build(); // HTTP 200

		} catch (Exception e) {
			log.error("Error handling webhook from {}", provider, e);
			// Still return 200 to prevent provider retries for unrecoverable errors
			// (Provider will stop retrying after max attempts anyway)
			return ResponseEntity.ok().build();
		}
	}
}