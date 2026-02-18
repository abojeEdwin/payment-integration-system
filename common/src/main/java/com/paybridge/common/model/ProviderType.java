package com.paybridge.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProviderType {
	PAYSTACK("Paystack"),
	INTERSWITCH("Interswitch"),
	SQUADCO("SquadCo"),
	FLUTTERWAVE("Flutterwave"),
	STRIPE("Stripe"),
	MOCK("Mock");

	private final String displayName;
}
