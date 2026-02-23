package com.paybridge.webhook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_webhooks", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWebhook {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 50)
	private String provider;

	@Column(name = "event_id", nullable = false, length = 255)
	private String eventId; // Provider's unique event ID or hash of payload

	@CreationTimestamp
	@Column(name = "processed_at", nullable = false, updatable = false)
	private Instant processedAt;
}
