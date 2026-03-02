package com.paybridge.payment.repository;

import com.paybridge.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

	/**
	 * Find transaction by idempotency key (critical for duplicate prevention)
	 */
	Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

	/**
	 * Find transaction by provider's external reference
	 */
	Optional<PaymentTransaction> findByExternalReference(String externalReference);

	/**
	 * Check if API key exists (simplified validation for Phase 1)
	 */
	boolean existsByApiKey(String apiKey);

	/**
	 * Find transactions by merchant ID
	 */
	@Query("SELECT t FROM PaymentTransaction t WHERE t.merchantId = :merchantId ORDER BY t.createdAt DESC")
	Iterable<PaymentTransaction> findByMerchantId(UUID merchantId);
}
