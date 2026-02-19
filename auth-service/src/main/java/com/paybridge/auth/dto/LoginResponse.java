package com.paybridge.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public final class LoginResponse {

	private String token;
	private UUID merchantId;
	private String merchantName;
	private String email;
}