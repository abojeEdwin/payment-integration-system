package com.paybridge.webhook.controller;

import com.paybridge.webhook.dto.WebhookPayload;
import com.paybridge.webhook.service.WebhookIngressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

	private final WebhookIngressService webhookService;

	@PostMapping("/{provider}")
	public ResponseEntity<Void> handleWebhook(
			@PathVariable String provider,
			@RequestBody String rawPayload) {

		webhookService.saveAndPublish(new WebhookPayload(provider, rawPayload));
		return ResponseEntity.ok().build(); // Acknowledge immediately
	}
}