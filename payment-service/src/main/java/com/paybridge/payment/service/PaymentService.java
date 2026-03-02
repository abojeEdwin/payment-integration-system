package com.paybridge.payment.service;

import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.util.IdempotencyKeyGenerator;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.entity.PaymentTransaction;
import com.paybridge.payment.event.PaymentEvent;
import com.paybridge.payment.repository.PaymentTransactionRepository;
import com.paybridge.payment.service.provider.PaymentProvider;
import com.paybridge.payment.service.provider.ProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentTransactionRepository        transactionRepository;
	private final ProviderFactory                     providerFactory;
	private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@Transactional
	public PaymentResponse processPayment(CreatePaymentRequest request, UUID merchantId, String apiKey) {
		log.info("Processing payment for merchant: {}", merchantId);

		// Step 1: Validate request
		validateRequest(request);

		// Step 2: Check for duplicate via idempotency key
		PaymentTransaction existing = transactionRepository
				.findByIdempotencyKey(request.getIdempotencyKey())
				.orElse(null);

		if (existing != null) {
			log.info("Idempotency key found - returning existing transaction: {}", existing.getId());
			return mapToResponse(existing);
		}

		// Step 3: Create new transaction record (PENDING state)
		PaymentTransaction transaction = createPendingTransaction(request, merchantId, apiKey);
		transaction = transactionRepository.save(transaction);
		log.info("Created pending transaction: {}", transaction.getId());

		// Step 4: Publish event BEFORE provider call (for audit trail)
		publishPaymentEvent(transaction, "PAYMENT_INITIATED");

		// Step 5: Charge via provider
		PaymentProvider provider = providerFactory.getProvider(request.getProvider());
		PaymentResponse providerResponse;

		try {
			providerResponse = provider.charge(request);
		} catch (Exception e) {
			log.error("Provider charge failed", e);
			transaction.updateStatus(PaymentStatus.FAILED, e.getMessage());
			transactionRepository.save(transaction);
			publishPaymentEvent(transaction, "PAYMENT_FAILED");
			throw new PaymentException(
					"Payment provider error: " + e.getMessage(),
					"PAY_003",
					ErrorCategory.SERVER_ERROR
			);
		}

		// Step 6: Update transaction with provider response
		transaction.updateStatus(
				providerResponse.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
				providerResponse.toString()
		);
		transaction.setExternalReference(providerResponse.getReference());
		transaction = transactionRepository.save(transaction);
		log.info("Transaction updated: {} -> {}", transaction.getId(), transaction.getStatus());

		// Step 7: Publish final event
		publishPaymentEvent(transaction,
				providerResponse.isSuccess() ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");

		return mapToResponse(transaction);
	}

	private void validateRequest(CreatePaymentRequest request) {
		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new PaymentException(
					"Amount must be greater than zero",
					"PAY_004",
					ErrorCategory.CLIENT_ERROR
			);
		}

		if (!IdempotencyKeyGenerator.isValid(request.getIdempotencyKey())) {
			throw new PaymentException(
					"Invalid idempotency key format",
					"PAY_005",
					ErrorCategory.CLIENT_ERROR
			);
		}
	}

	/**
     * Get payment status with merchant ownership validation
     */
    @Transactional(readOnly = true)
	public PaymentResponse getPaymentStatus(UUID transactionId, String apiKey) {
		PaymentTransaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new PaymentException(
						"Transaction not found", "PAY_007",
						ErrorCategory.CLIENT_ERROR
				));

		if (!transaction.getApiKey().equals(apiKey)) {
			throw new PaymentException(
					"Unauthorized access", "AUTH_009",
					ErrorCategory.CLIENT_ERROR
			);
		}

		return mapToResponse(transaction); // Auto-mapped!
	}

	private PaymentTransaction createPendingTransaction(
			CreatePaymentRequest request,
			UUID merchantId,
			String apiKey) {

		return PaymentTransaction.builder()
				.merchantId(merchantId)
				.apiKey(apiKey)
				.amount(request.getAmount())
				.currency(request.getCurrency())
				.customerEmail(request.getCustomerEmail())
				.description(request.getDescription())
				.status(PaymentStatus.INITIATED)
				.provider(request.getProvider())
				.externalReference("pending_" + System.currentTimeMillis())
				.idempotencyKey(request.getIdempotencyKey())
				.build();
	}

	private PaymentResponse mapToResponse(PaymentTransaction transaction) {
		return PaymentResponse.builder()
				.success(transaction.getStatus() == PaymentStatus.SUCCESS)
				.transactionId(transaction.getId().toString())
				.reference(transaction.getExternalReference())
				.status(transaction.getStatus())
				.amount(transaction.getAmount())
				.currency(transaction.getCurrency())
				.customerEmail(transaction.getCustomerEmail())
				.message(getStatusMessage(transaction.getStatus()))
				.authorizationUrl(
						transaction.getStatus() == PaymentStatus.INITIATED ?
								"https://mock.pay/auth/" + transaction.getId() : null
				)
				.build();
	}

	private String getStatusMessage(PaymentStatus status) {
		return switch (status) {
			case INITIATED -> "Payment initiated";
			case PROCESSING -> "Payment processing";
			case SUCCESS -> "Payment successful";
			case FAILED -> "Payment failed";
			case REFUNDED -> "Payment refunded";
			case CANCELLED -> "Payment cancelled";
		};
	}

	private void publishPaymentEvent(PaymentTransaction transaction, String eventType) {
		try {
			PaymentEvent event = PaymentEvent.builder()
					.eventId(UUID.randomUUID().toString())
					.eventType(eventType)
					.transactionId(transaction.getId().toString())
					.merchantId(transaction.getMerchantId().toString())
					.amount(transaction.getAmount())
					.currency(transaction.getCurrency().toString())
					.status(transaction.getStatus().toString())
					.provider(transaction.getProvider().toString())
					.timestamp(java.time.Instant.now())
					.build();

			kafkaTemplate.send("payment-events", event.getEventId(), event);
			log.debug("Published event: {} for transaction {}", eventType, transaction.getId());
		} catch (Exception e) {
			log.warn("Failed to publish Kafka event (non-fatal)", e);
			// Don't fail payment if event publishing fails - it's async anyway
		}
	}
}