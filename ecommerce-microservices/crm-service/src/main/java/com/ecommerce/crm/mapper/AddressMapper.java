package com.ecommerce.crm.mapper;

import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.dto.response.AddressResponse;
import com.ecommerce.crm.model.Address;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    /**
     * AddressRequest DTO -> Address Entity Dönüşümü
     */
    @Mapping(target = "addressTitle", source = "title")
    @Mapping(target = "addressLine", source = "street")
    @Mapping(target = "district", source = "state")
    Address toEntity(AddressRequest request);

    /**
     * Address Entity -> AddressResponse DTO Dönüşümü
     */
    AddressResponse toResponse(Address entity);

    /**
     * Entity Listesi -> Response DTO Listesi Dönüşümü
     */
    List<AddressResponse> toResponseList(List<Address> entities);

    /**
     * PATCH Kısmi Güncelleme
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "addressTitle", source = "title")
    @Mapping(target = "addressLine", source = "street")
    @Mapping(target = "district", source = "state")
    void updateEntityFromRequest(AddressRequest request, @MappingTarget Address entity);
}