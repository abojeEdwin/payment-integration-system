package com.paybridge.auth.service;

import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiKeyMapper {

	@Mapping(source = "keyValue", target = "keyValue") // Map entity field to DTO field
	@Mapping(source = "keyPrefix", target = "keyPrefix")
	@Mapping(source = "description", target = "description")
	@Mapping(source = "active", target = "active")
	@Mapping(source = "expiresAt", target = "expiresAt")
	@Mapping(source = "createdAt", target = "createdAt")
	@Mapping(source = "lastUsedAt", target = "lastUsedAt")
	ApiKeyResponse toResponse(ApiKey apiKey);
}
