
package com.paybridge.payment.application.port.gateway;

import com.paybridge.common.model.ChargeCommand;
import com.paybridge.payment.domain.dto.PaymentResult;

public interface PaymentGateway {
    PaymentResult charge(ChargeCommand command);
    PaymentResult refund(String transactionId, Double amount);
}