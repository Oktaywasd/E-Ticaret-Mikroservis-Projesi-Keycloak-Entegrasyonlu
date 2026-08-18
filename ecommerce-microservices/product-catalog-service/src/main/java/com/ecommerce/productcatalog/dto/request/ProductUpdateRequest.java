package com.ecommerce.productcatalog.dto.request;
import com.ecommerce.productcatalog.dto.PriceDto;
import com.ecommerce.productcatalog.dto.StockDto;
import com.ecommerce.productcatalog.dto.VariantDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotBlank(message = "Kategori id boş olamaz")
    private String categoryId;

    @NotBlank(message = "Marka boş olamaz")
    private String brand;

    @Valid // zorunluluk belritir
    @NotNull(message = "Fiyat bilgisi zorunludur")
    private PriceDto price;

    @Valid
    @NotNull(message = "Stok bilgisi zorunludur")
    private StockDto stock;

    @Valid
    private List<VariantDto> variants;
}
