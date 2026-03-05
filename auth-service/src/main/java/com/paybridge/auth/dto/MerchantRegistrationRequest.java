package com.paybridge.auth.dto;

import com.paybridge.common.model.ProviderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantRegistrationRequest {
	@NotBlank(message = "Name is required")
	private String       name;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String       email;

	@NotBlank(message = "Password is required")
	private String       password;

	@NotNull(message = "Provider is required")
	private ProviderType provider;
}
