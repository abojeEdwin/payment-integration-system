package com.paybridge.auth.entity;

import com.paybridge.common.model.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchants", uniqueConstraints = {
		@UniqueConstraint(columnNames = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash; // BCrypt hashed password

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProviderType paymentProvider;

	@Column(nullable = false)
	private boolean active = true;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ApiKey> apiKeys = new ArrayList<>();

	/**
	 * Check if merchant can authenticate
	 */
	public boolean canAuthenticate() {
		return this.active;
	}

	/**
	 * Update merchant details
	 */
	public void updateDetails(String name, ProviderType provider) {
		this.name = name;
		this.paymentProvider = provider;
	}
}