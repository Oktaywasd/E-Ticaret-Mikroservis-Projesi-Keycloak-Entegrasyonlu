package com.ecommerce.productcatalog.mapper;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    Product toEntity(ProductCreateRequest request);

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productCode", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    Product updateEntityFromRequest(ProductUpdateRequest request, @org.mapstruct.MappingTarget Product product);
}