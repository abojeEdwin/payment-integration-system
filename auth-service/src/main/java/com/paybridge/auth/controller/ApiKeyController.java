package com.paybridge.auth.controller;

import com.paybridge.auth.dto.ApiKeyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
	@PostMapping
	public ResponseEntity<ApiKeyResponse> generateApiKey() {
		return ResponseEntity.ok(new ApiKeyResponse("generated-api-key"));
	}
}