package com.ecommerce.productcatalog.mapper;

import com.ecommerce.productcatalog.dto.request.CategoryCreateRequest;
import com.ecommerce.productcatalog.dto.response.CategoryResponse;
import com.ecommerce.productcatalog.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryCreateRequest request);

    CategoryResponse toResponse(Category category);
}
