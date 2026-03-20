package com.paybridge.common.exception;

import com.paybridge.common.model.ErrorCategory;
import lombok.Getter;

@Getter
public class InsufficientFundsException extends PaymentException {
	private final double requestedAmount;
	private final double availableBalance;

	public InsufficientFundsException(double requestedAmount, double availableBalance) {
		super(String.format("Insufficient funds. Requested: %.2f, Available: %.2f",
						requestedAmount, availableBalance),
				"PAY_002", ErrorCategory.BUSINESS_ERROR);
		this.requestedAmount = requestedAmount;
		this.availableBalance = availableBalance;
	}

}
