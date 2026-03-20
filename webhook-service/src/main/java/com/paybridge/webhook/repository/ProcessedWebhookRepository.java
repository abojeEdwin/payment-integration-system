package com.paybridge.webhook.repository;

import com.paybridge.webhook.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {

	/**
	 * Check if webhook was already processed (idempotency)
	 */
	boolean existsByProviderAndEventId(String provider, String eventId);

	/**
	 * Record processed webhook
	 */
	ProcessedWebhook save(ProcessedWebhook processedWebhook);
}