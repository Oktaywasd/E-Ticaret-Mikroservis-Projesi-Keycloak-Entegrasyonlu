package com.ecommerce.productcatalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {

    @NotNull(message = "Stok miktarı zorunludur")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer currentStock;

    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer minimumStock;
}