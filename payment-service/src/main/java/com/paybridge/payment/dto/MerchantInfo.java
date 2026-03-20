package com.paybridge.payment.dto;

import com.paybridge.common.model.ProviderType;
import lombok.Data;

@Data
public class MerchantInfo {
	private String       merchantId;
	private ProviderType provider;
	private boolean      active;
}