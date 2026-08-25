package com.ecommerce.productcatalog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed(unique = true) // İş kuralı: Ürün kodu benzersiz olmalı
    private String productCode;

    private String name;
    private String description;
    private String categoryId;
    private String brand;

    private Boolean isDeleted = false; // Soft Delete için flag

    private List<Variant> variants;
    private Price price;
    private Stock stock;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    private Double ratingAverage = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}