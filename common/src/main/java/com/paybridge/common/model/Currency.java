package com.paybridge.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported currencies with ISO codes.
 * <p>
 * EDUCATION:
 * - Store ISO code (e.g., "NGN") → standard format
 * - Add symbol() → for UI display
 * - Add decimalPlaces() → for amount formatting
 */
@RequiredArgsConstructor
@Getter
public enum Currency {

	NGN("₦", 2),
	USD("$", 2),
	EUR("€", 2),
	GBP("£", 2),
	GHS("₵", 2);

	private final String symbol;
	private final int    decimalPlaces;

	@Override
	public String toString() {
		return name();
	}
}
