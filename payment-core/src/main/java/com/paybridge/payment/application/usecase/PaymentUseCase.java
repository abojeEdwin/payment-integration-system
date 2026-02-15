package com.paybridge.payment.application.usecase;

import com.paybridge.common.exception.InvalidAmountException;
import com.paybridge.common.model.ChargeCommand;
import com.paybridge.common.model.ChargeResponse;
import com.paybridge.payment.application.port.gateway.PaymentGateway;
import com.paybridge.payment.application.port.spi.TransactionRepository;
import com.paybridge.payment.domain.model.*;
import com.paybridge.payment.domain.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentUseCase {

	private final PaymentGateway        paymentGateway;
	private final TransactionRepository transactionRepository;

	public ChargeResponse execute(ChargeCommand command) {
		// 1. Business validation (pure domain logic)
		if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Amount must be greater than zero.");
		}

		// 2. Execute payment via the selected PSP adapter
		PaymentResult paymentResult = paymentGateway.charge(command);

		// 3. Persist transaction record
		Transaction transaction = new Transaction(
				command.merchantId(),
				"PAYSTACK", // This could come from merchant config
				command.reference(),
				command.amount(),
				command.currency(),
				paymentResult.status(),
				command.customerEmail()
		);
		transactionRepository.save(transaction);

		return new ChargeResponse(transaction.id(), paymentResult.status());

	}
}
