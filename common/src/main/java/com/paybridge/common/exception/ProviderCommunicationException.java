package com.paybridge.common.exception;

public class ProviderCommunicationException extends CommonException {
	private final String providerName;

    public ProviderCommunicationException(String providerName, String message, Throwable cause) {
        super("Provider [" + providerName + "] communication failed: " + message,
              cause,
              org.springframework.http.HttpStatus.BAD_GATEWAY, "PROVIDER_001");
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
