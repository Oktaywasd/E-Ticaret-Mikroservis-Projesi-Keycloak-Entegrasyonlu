package com.ecommerce.crm.mapper;

import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface UserProfileMapper {

    UserProfile toEntity(UserRegisterRequest request);

    UserProfileResponse toResponse(UserProfile entity);

    void updateEntityFromRequest(UserProfileUpdateRequest request, @MappingTarget UserProfile entity);
}