package com.paybridge.webhook.processor.application.port;

import java.util.UUID;

public interface TransactionUpdatePort {
	void updateStatus(UUID transactionId, String newStatus);
}