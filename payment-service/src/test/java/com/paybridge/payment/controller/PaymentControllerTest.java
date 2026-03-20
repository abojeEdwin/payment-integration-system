package com.paybridge.payment.controller;


import com.paybridge.common.model.Currency;
import com.paybridge.common.model.PaymentStatus;
import com.paybridge.common.model.ProviderType;
import com.paybridge.common.util.IdempotencyKeyGenerator;
import com.paybridge.payment.dto.CreatePaymentRequest;
import com.paybridge.payment.dto.PaymentResponse;
import com.paybridge.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /payments: Should process payment successfully")
    void createPayment_Success() throws Exception {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency(Currency.NGN);
        request.setCustomerEmail("test@example.com");
        request.setDescription("Test order");
        request.setProvider(ProviderType.MOCK);
        request.setIdempotencyKey(IdempotencyKeyGenerator.generate());

        PaymentResponse response = PaymentResponse.builder()
                .success(true)
                .transactionId(UUID.randomUUID().toString())
                .reference("ref_123")
                .status(PaymentStatus.SUCCESS)
                .build();

        given(paymentService.processPayment(any(), any(), any()))
                .willReturn(response);

        // When/Then
        mockMvc.perform(
                post("/payments")
                        .header("X-API-Key", "pk_test_abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "amount": 1000.00,
                                "currency": "NGN",
                                "customerEmail": "test@example.com",
                                "description": "Test order",
                                "provider": "MOCK",
                                "idempotencyKey": "%s"
                            }
                            """.formatted(request.getIdempotencyKey()))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /payments: Should return 400 for invalid request")
    void createPayment_ValidationFails() throws Exception {
        // When/Then
        mockMvc.perform(
                post("/payments")
                        .header("X-API-Key", "pk_test_abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}") // Empty payload
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.amount").exists());
    }
}