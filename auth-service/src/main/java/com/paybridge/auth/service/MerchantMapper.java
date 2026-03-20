package com.paybridge.auth.service;

import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.auth.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MerchantMapper {

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "paymentProvider", target = "paymentProvider")
	@Mapping(source = "active", target = "active")
	@Mapping(source = "createdAt", target = "createdAt")
	@Mapping(source = "updatedAt", target = "updatedAt")
	MerchantDto toDto(Merchant merchant);
}
