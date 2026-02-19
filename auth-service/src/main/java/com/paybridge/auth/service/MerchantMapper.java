package com.paybridge.auth.service;

import com.paybridge.auth.dto.MerchantDto;
import com.paybridge.auth.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MerchantMapper {
	MerchantMapper INSTANCE = Mappers.getMapper(MerchantMapper.class);

    MerchantDto toDto(Merchant merchant);
}
