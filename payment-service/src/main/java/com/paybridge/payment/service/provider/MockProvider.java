package com.paybridge.payment.service.provider;

import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.dto.PaymentStatusUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MockProvider implements PaymentProvider{
	@Override
	public PaymentResponse charge(CreatePaymentRequest request) {
		log.info("MockProvider processing payment: {}", request);

		// Simulate 80% success rate for realism
		boolean success = Math.random() > 0.2;

		return PaymentResponse.builder()
				.success(success)
				.transactionId("mock_txn_" + UUID.randomUUID())
				.reference("mock_ref_" + UUID.randomUUID())
				.status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
				.message(success ? "Mock payment succeeded" : "Mock payment failed")
				.authorizationUrl(success ? "https://mock.pay/auth" : null)
				.build();
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.MOCK;
	}
}
