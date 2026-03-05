package com.paybridge.payment.controller;

import com.paybridge.common.dto.ApiResponse;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.dto.PaymentStatusUpdateRequest;
import com.paybridge.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
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
			HttpServletRequest httpRequest) {

		String merchantIdStr = (String) httpRequest.getAttribute("MERCHANT_ID");
		UUID merchantId = UUID.fromString(merchantIdStr);

		log.info("Received payment request: {}", request);
		PaymentResponse response = paymentService.processPayment(
				request,
				merchantId,
				httpRequest.getHeader("X-API-Key"));

		return ResponseEntity.ok(
				ApiResponse.success(
						response,
						"Payment processed successfully"));
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
}