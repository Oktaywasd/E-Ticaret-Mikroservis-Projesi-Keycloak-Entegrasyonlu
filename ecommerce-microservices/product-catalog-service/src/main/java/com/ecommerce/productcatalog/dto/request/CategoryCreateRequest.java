package com.ecommerce.productcatalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class CategoryCreateRequest {
    @NotBlank(message = "kategori adı baş olamaz")
    private String name;
    private String parentCategoryId;
}
