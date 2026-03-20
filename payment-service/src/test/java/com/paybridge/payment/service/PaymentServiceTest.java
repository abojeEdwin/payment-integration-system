package com.paybridge.payment.service;

import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.Currency;
import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import com.paybridge.common.util.IdempotencyKeyGenerator;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.entity.PaymentTransaction;
import com.paybridge.payment.event.PaymentEvent;
import com.paybridge.payment.repository.PaymentTransactionRepository;
import com.paybridge.payment.service.provider.MockProvider;
import com.paybridge.payment.service.provider.ProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private ProviderFactory providerFactory;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Mock
    private MockProvider mockProvider;

    @InjectMocks
    private PaymentService paymentService;

    private CreatePaymentRequest validRequest;
    private UUID merchantId;
    private String apiKey;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        apiKey = "pk_test_" + "x".repeat(56);

        validRequest = new CreatePaymentRequest();
        validRequest.setAmount(new BigDecimal("1500.00"));
        validRequest.setCurrency(Currency.NGN);
        validRequest.setCustomerEmail("customer@example.com");
        validRequest.setDescription("Test payment");
        validRequest.setProvider(ProviderType.MOCK);
        validRequest.setIdempotencyKey(IdempotencyKeyGenerator.generate());

        given(providerFactory.getProvider(ProviderType.MOCK)).willReturn(mockProvider);

        PaymentResponse mockResponse = PaymentResponse.builder()
                .success(true)
                .transactionId("mock_txn_123")
                .reference("mock_ref_123")
                .status(PaymentStatus.SUCCESS)
                .build();
        given(mockProvider.charge(any())).willReturn(mockResponse);
    }

    @Test
    @DisplayName("processPayment: Should create new transaction for unique idempotency key")
    void processPayment_NewTransaction() {
        // Given
        given(transactionRepository.findByIdempotencyKey(validRequest.getIdempotencyKey()))
                .willReturn(Optional.empty());

        given(transactionRepository.save(any(PaymentTransaction.class)))
                .willAnswer(invocation -> {
                    PaymentTransaction tx = invocation.getArgument(0);
                    if (tx.getId() == null) {
                        tx.setId(UUID.randomUUID());
                    }
                    return tx;
                });

        // When
        PaymentResponse response = paymentService.processPayment(validRequest, merchantId, apiKey);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        verify(transactionRepository).save(argThat(tx ->
            tx.getStatus() == PaymentStatus.SUCCESS &&
            tx.getMerchantId().equals(merchantId)
        ));
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("processPayment: Should return existing result for duplicate idempotency key")
    void processPayment_IdempotencyPreventsDuplicate() {
        // Given: Existing successful transaction
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .amount(validRequest.getAmount())
                .currency(validRequest.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .externalReference("existing_ref")
                .idempotencyKey(validRequest.getIdempotencyKey())
                .build();

        given(transactionRepository.findByIdempotencyKey(validRequest.getIdempotencyKey()))
                .willReturn(Optional.of(existing));

        // When
        PaymentResponse response = paymentService.processPayment(validRequest, merchantId, apiKey);

        // Then: No new charge, no new save
        assertThat(response).isNotNull();
        assertThat(response.getReference()).isEqualTo("existing_ref");
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
        verify(mockProvider, never()).charge(any());
    }

    @Test
    @DisplayName("processPayment: Should fail for zero/negative amount")
    void processPayment_InvalidAmount() {
        // Given
        validRequest.setAmount(BigDecimal.ZERO);

        // When/Then
        assertThatThrownBy(() ->
            paymentService.processPayment(validRequest, merchantId, apiKey)
        ).isInstanceOf(PaymentException.class)
         .hasMessageContaining("Amount must be greater than zero");
    }
}