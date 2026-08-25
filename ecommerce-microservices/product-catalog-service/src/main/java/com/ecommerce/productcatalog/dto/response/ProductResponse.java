package com.ecommerce.productcatalog.dto.response;

import com.ecommerce.productcatalog.dto.PriceDto;
import com.ecommerce.productcatalog.dto.StockDto;
import com.ecommerce.productcatalog.dto.VariantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String productCode;
    private String name;
    private String description;
    private String categoryId;
    private String brand;
    private PriceDto price;
    private StockDto stock;
    private List<VariantDto> variants;

    // --- görsel ekleme ---
    private List<String> imageUrls;
    private Double ratingAverage;
    private Integer reviewCount;
    // ----------------------------

    // --- Durum ve Silinme Bilgileri ---
    private Boolean isActive;
    private Boolean isDeleted;
    // ---------------------------------

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}