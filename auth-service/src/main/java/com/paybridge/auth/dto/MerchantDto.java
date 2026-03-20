package com.paybridge.auth.dto;



import com.paybridge.common.model.ProviderType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public final class MerchantDto {

    private UUID id;
    private String name;
    private String email;
    private ProviderType paymentProvider;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}