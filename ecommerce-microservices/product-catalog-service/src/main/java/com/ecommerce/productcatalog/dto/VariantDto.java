package com.ecommerce.productcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantDto {

    @NotBlank(message = "Varyant adı baş olamaz (renk,benden vb.")
    private String name;

    @NotBlank(message = "Varyant degeri baş olamaz (kırmızı, XL vb.")
    private String value;
}
