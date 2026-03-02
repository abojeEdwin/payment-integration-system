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

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	/**
	 * Create new payment
	 * POST /payments
	 * <p>
	 * Headers:
	 * X-API-Key: {merchant_api_key}
	 * <p>
	 * Request Body:
	 * {
	 * "amount": 1500.00,
	 * "currency": "NGN",
	 * "customerEmail": "customer@example.com",
	 * "description": "Order #123",
	 * "provider": "PAYSTACK",
	 * "idempotencyKey": "idemp_20240115123456_abc123XYZ789"
	 * }
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
			@Valid @RequestBody CreatePaymentRequest request,
			@RequestHeader("X-API-Key") String apiKey,
			Principal principal) {

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

		// Simplified: fetch from DB
		// In real system: validate merchant owns this transaction
		var transaction = paymentService.getTransaction(transactionId); // Add this method to PaymentService

		return ResponseEntity.ok(ApiResponse.success(mapToResponse(transaction)));
	}

	// Simplified merchant ID derivation (Phase 1)
	private UUID deriveMerchantIdFromApiKey(String apiKey) {
		// In production: call auth-service to get merchant ID
		// For now: use deterministic UUID based on API key prefix
		String prefix = apiKey.substring(0, Math.min(apiKey.length(), 8));
		return UUID.nameUUIDFromBytes(prefix.getBytes());
	}

//	@PostMapping
//	public ResponseEntity<ApiResponse<PaymentResponse>>
//	createPayment(@Valid @RequestBody PaymentRequest request) {
//		PaymentResponse response = paymentService.processPayment(request);
//		return ResponseEntity.ok(ApiResponse.success(response));
//	}


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

}