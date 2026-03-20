package com.paybridge.common.exception;

import com.paybridge.common.model.ErrorCategory;
import lombok.Getter;

@Getter
public class ProviderCommunicationException extends PaymentException {
	private final String providerName;

    public ProviderCommunicationException(String providerName, String message, Throwable cause) {
        super("Provider [" + providerName + "] communication failed: " + message,
              cause, "PROVIDER_001", ErrorCategory.SERVER_ERROR);
        this.providerName = providerName;
    }

}
