package com.paybridge.webhook.ingress.application.controller;


import com.paybridge.webhook.ingress.application.usecase.WebhookIngressUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookIngressController {

	private final WebhookIngressUseCase webhookIngressUseCase;

	public WebhookIngressController(WebhookIngressUseCase webhookIngressUseCase) {
		this.webhookIngressUseCase = webhookIngressUseCase;
	}

	@PostMapping("/paystack")
	public ResponseEntity<Void> handlePaystackWebhook(@RequestBody String payload, @RequestHeader Map<String, String> headers) {
		webhookIngressUseCase.receiveWebhook("PAYSTACK", payload, headers);
		return ResponseEntity.ok().build(); // Immediate 200 OK
	}
}