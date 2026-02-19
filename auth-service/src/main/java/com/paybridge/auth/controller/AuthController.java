package com.paybridge.auth.controller;

import com.paybridge.auth.config.JwtTokenProvider;
import com.paybridge.auth.dto.LoginRequest;
import com.paybridge.auth.dto.LoginResponse;
import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.service.AuthService;
import com.paybridge.common.dto.ApiResponse;
import com.paybridge.common.model.ProviderType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final AuthService authService;
	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * Register new merchant
	 * POST /auth/register
	 */
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<MerchantDto>> registerMerchant(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam ProviderType provider) {

		MerchantDto merchant = authService.registerMerchant(name, email, password, provider);
		return ResponseEntity.ok(ApiResponse.success(merchant, "Merchant registered successfully"));
	}

	/**
	 * Login merchant (get JWT token)
	 * POST /auth/login
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		// Authenticate
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		// Generate JWT
		String jwt = jwtTokenProvider.generateToken(authentication);

		// Get merchant details
		Merchant merchant = authService.authenticate(request.getEmail(), request.getPassword());

		LoginResponse response = LoginResponse.builder()
				.token(jwt)
				.merchantId(merchant.getId())
				.merchantName(merchant.getName())
				.email(merchant.getEmail())
				.build();

		return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
	}

	/**
	 * Get current merchant
	 * GET /auth/me
	 */
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MerchantDto>> getCurrentMerchant(Authentication authentication) {
		UUID merchantId = UUID.fromString(authentication.getName());
		MerchantDto merchant = authService.getMerchant(merchantId);
		return ResponseEntity.ok(ApiResponse.success(merchant));
	}

	/**
	 * Update merchant details
	 * PUT /auth/me
	 */
	@PutMapping("/me")
	public ResponseEntity<ApiResponse<MerchantDto>> updateMerchant(
			Authentication authentication,
			@RequestParam String name,
			@RequestParam ProviderType provider) {

		UUID merchantId = UUID.fromString(authentication.getName());
		MerchantDto merchant = authService.updateMerchant(merchantId, name, provider);
		return ResponseEntity.ok(ApiResponse.success(merchant, "Merchant updated successfully"));
	}
}
