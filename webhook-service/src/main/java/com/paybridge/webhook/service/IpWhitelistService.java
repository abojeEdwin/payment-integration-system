package com.paybridge.webhook.service;

import com.paybridge.webhook.config.IpWhitelistConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IpWhitelistService {

	private final IpWhitelistConfig whitelistConfig;

	public boolean isWhitelisted(@NonNull String ip) {
		return whitelistConfig.getWhitelist().contains(ip);
	}
}
//The IP whitelist check performs simple string matching which may not handle IPv6 addresses,
// CIDR notation, or IP ranges correctly. Consider using IP address parsing libraries
// to support these formats and handle edge cases like IPv4-mapped IPv6 addresses.