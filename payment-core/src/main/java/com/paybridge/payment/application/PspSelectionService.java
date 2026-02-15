package com.paybridge.payment.application;

import com.paybridge.payment.application.port.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PspSelectionService {

	private final Map<String, PaymentGateway> gateways;

	public PaymentGateway select(String providerType) {

		return gateways.getOrDefault(
				providerType,
				gateways.get("paystack")); //Default to Paystack if provider type is not specified or unsupported
	}
}
