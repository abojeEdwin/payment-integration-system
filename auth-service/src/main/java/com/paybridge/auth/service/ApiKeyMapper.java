package com.paybridge.auth.service;

import com.paybridge.auth.dto.ApiKeyResponse;
import com.paybridge.auth.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiKeyMapper {
	ApiKeyMapper INSTANCE = Mappers.getMapper(ApiKeyMapper.class);

	ApiKeyResponse toResponse(ApiKey apiKey);
}
