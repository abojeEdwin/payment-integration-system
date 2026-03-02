package com.paybridge.payment.controller;

import com.paybridge.common.dto.ApiResponse;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.dto.PaymentStatusUpdateRequest;
import com.paybridge.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
			@Valid @RequestBody CreatePaymentRequest request,
			@RequestHeader("X-API-Key") String apiKey) {

		log.info("Received payment request: {}", request);

		// Generate merchant ID from API key (simplified - in real system call auth-service)
		UUID merchantId = deriveMerchantIdFromApiKey(apiKey);

		// Process payment with idempotency
		PaymentResponse response = paymentService.processPayment(request, merchantId, apiKey);

		return ResponseEntity.ok(ApiResponse.success(response, "Payment processed successfully"));
	}

	/**
	 * Get payment status
	 * GET /payments/{transactionId}
	 */
	@GetMapping("/{transactionId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
			@PathVariable UUID transactionId,
			@RequestHeader("X-API-Key") String apiKey) {
		log.info("Fetching status for transaction: {}", transactionId);

		PaymentResponse response = paymentService.getPaymentStatus(transactionId, apiKey);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * INTERNAL ENDPOINT: Update payment status (called by webhook-service)
	 * ⚠️ NO AUTHENTICATION IN PHASE 1 - SECURE IN PHASE 2
	 * Phase 2: Add service-to-service auth (JWT/IP whitelist/mTLS)
	 */
	@PatchMapping("/internal/payments/status")
	public ResponseEntity<ApiResponse<Void>> updatePaymentStatus(
			@RequestBody PaymentStatusUpdateRequest request) {
		log.info("Received status update request for reference: {}",
				request.getTransactionReference());

		paymentService.updatePaymentStatus(
				request.getTransactionReference(),
				request.getNewStatus(),
				request.getProviderResponse()
		);
		return ResponseEntity.ok(ApiResponse.<Void>builder()
				.build());
	}

	// Simplified merchant ID derivation (Phase 1)
	private UUID deriveMerchantIdFromApiKey(String apiKey) {
		String prefix = apiKey.substring(0, Math.min(apiKey.length(), 8));
		return UUID.nameUUIDFromBytes(prefix.getBytes());
	}
}