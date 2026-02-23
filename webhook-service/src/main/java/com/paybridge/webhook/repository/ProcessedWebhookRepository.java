package com.paybridge.webhook.repository;

import com.paybridge.webhook.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {

	/**
	 * Check if webhook was already processed (idempotency)
	 */
	boolean existsByProviderAndEventId(String provider, String eventId);

	/**
	 * Record processed webhook
	 */
	ProcessedWebhook save(ProcessedWebhook processedWebhook);
}