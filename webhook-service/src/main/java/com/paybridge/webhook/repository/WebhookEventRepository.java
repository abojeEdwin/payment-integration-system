package com.paybridge.webhook.repository;

import com.paybridge.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

	/**
	 * Find pending events for retry processing
	 */
	@Query("SELECT w FROM WebhookEvent w WHERE w.status = 'PENDING' OR w.status = 'PROCESSING' ORDER BY w.receivedAt ASC")
	List<WebhookEvent> findPendingEvents();

	/**
	 * Count events by status (for monitoring)
	 */
	long countByStatus(String status);
}