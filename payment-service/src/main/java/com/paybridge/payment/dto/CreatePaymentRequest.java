package com.paybridge.payment.dto;

import com.paybridge.common.model.Currency;
import com.paybridge.common.model.ProviderType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	private BigDecimal amount;

	@NotNull(message = "Currency is required")
	private Currency currency;

	@NotBlank(message = "Customer email is required")
	@Email(message = "Invalid email format")
	private String customerEmail;

	@NotBlank(message = "Description is required")
	@Size(max = 255, message = "Description must be less than 255 characters")
	private String description;

	@NotNull(message = "Provider is required")
	private ProviderType provider;

	@NotBlank(message = "Idempotency key is required")
	@Pattern(regexp = "^idemp_\\d{14}_[A-Za-z0-9]{16}$",
			message = "Invalid idempotency key format")
	private String idempotencyKey;
}