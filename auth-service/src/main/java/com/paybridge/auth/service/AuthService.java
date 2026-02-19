package com.paybridge.auth.service;

import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.MerchantRepository;
import com.paybridge.common.exception.PaymentException;
import com.paybridge.common.model.ErrorCategory;
import com.paybridge.common.model.ProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantMapper merchantMapper;

    /**
     * Register new merchant
     */
    @Transactional
    public MerchantDto registerMerchant(
			String name, String email, String password, ProviderType provider) {
        // Validate email uniqueness
        if (merchantRepository.existsByEmail(email)) {
            throw new PaymentException("Email already registered", "AUTH_002",
                ErrorCategory.CLIENT_ERROR);
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(password);

        // Create merchant
        Merchant merchant = Merchant.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordHash)
                .paymentProvider(provider)
                .active(true)
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Merchant registered: {}", merchant.getId());

        return merchantMapper.toDto(merchant);
    }

    /**
     * Authenticate merchant (for admin portal)
     */
    public Merchant authenticate(String email, String password) {
        Merchant merchant = merchantRepository.findActiveByEmail(email)
                .orElseThrow(() -> new PaymentException(
                    "Invalid credentials", "AUTH_003",
                    ErrorCategory.CLIENT_ERROR));

        if (!passwordEncoder.matches(password, merchant.getPasswordHash())) {
            throw new PaymentException(
                "Invalid credentials", "AUTH_003",
                ErrorCategory.CLIENT_ERROR);
        }

        if (!merchant.canAuthenticate()) {
            throw new PaymentException(
                "Account is inactive", "AUTH_004",
                ErrorCategory.CLIENT_ERROR);
        }

        return merchant;
    }

    /**
     * Get merchant by ID
     */
    public MerchantDto getMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new PaymentException(
                    "Merchant not found", "AUTH_005",
                    ErrorCategory.CLIENT_ERROR));
        return merchantMapper.toDto(merchant);
    }

    /**
     * Update merchant details
     */
    @Transactional
    public MerchantDto updateMerchant(
			UUID merchantId, String name, ProviderType provider) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new PaymentException(
                    "Merchant not found", "AUTH_005",
                    ErrorCategory.CLIENT_ERROR));

        merchant.updateDetails(name, provider);
        merchant = merchantRepository.save(merchant);
        log.info("Merchant updated: {}", merchantId);

        return merchantMapper.toDto(merchant);
    }

    /**
     * Deactivate merchant
     */
    @Transactional
    public void deactivateMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new PaymentException(
                    "Merchant not found", "AUTH_005",
                    ErrorCategory.CLIENT_ERROR));

        merchant.setActive(false);
        merchantRepository.save(merchant);
        log.info("Merchant deactivated: {}", merchantId);
    }

    /**
     * Get all merchants (admin only)
     */
    public List<MerchantDto> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(merchantMapper::toDto)
                .toList();
    }
}