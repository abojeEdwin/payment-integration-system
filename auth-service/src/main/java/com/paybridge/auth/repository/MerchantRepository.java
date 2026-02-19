package com.paybridge.auth.repository;

import com.paybridge.auth.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

	/**
	 * Find merchant by email
	 */
	Optional<Merchant> findByEmail(String email);

	/**
	 * Find active merchant by email
	 */
	@Query("SELECT m FROM Merchant m WHERE m.email = :email AND m.active = true")
	Optional<Merchant> findActiveByEmail(String email);

	/**
	 * Check if email exists
	 */
	boolean existsByEmail(String email);
}