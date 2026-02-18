package com.paybridge.common.exception;

public class InsufficientFundsException extends CommonException {
	private final double requestedAmount;
	private final double availableBalance;

	public InsufficientFundsException(double requestedAmount, double availableBalance) {
		super(String.format("Insufficient funds. Requested: %.2f, Available: %.2f",
						requestedAmount, availableBalance),
				org.springframework.http.HttpStatus.BAD_REQUEST, "PAY_002");
		this.requestedAmount = requestedAmount;
		this.availableBalance = availableBalance;
	}

	public double getRequestedAmount() {
		return requestedAmount;
	}

	public double getAvailableBalance() {
		return availableBalance;
	}
}
