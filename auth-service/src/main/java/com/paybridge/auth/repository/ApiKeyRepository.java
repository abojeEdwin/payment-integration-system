package com.paybridge.auth.repository;

import com.paybridge.auth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

	/**
	 * Find API key by full key value
	 */
	Optional<ApiKey> findByKeyValue(String keyValue);

	/**
	 * Find active API key by full key value
	 */
	@Query("SELECT k FROM ApiKey k WHERE k.keyValue = :keyValue AND k.active = true")
	Optional<ApiKey> findActiveByKeyValue(String keyValue);

	/**
	 * Find API keys by merchant
	 */
	List<ApiKey> findByMerchantId(UUID merchantId);

	/**
	 * Find active API keys by merchant
	 */
	List<ApiKey> findByMerchantIdAndActive(UUID merchantId, boolean active);

	/**
	 * Count active keys for merchant
	 */
	long countByMerchantIdAndActive(UUID merchantId, boolean active);

	/**
	 * Find active API keys by key prefix (for efficient lookup)
	 */
	List<ApiKey> findActiveByKeyPrefix(String keyPrefix);

}