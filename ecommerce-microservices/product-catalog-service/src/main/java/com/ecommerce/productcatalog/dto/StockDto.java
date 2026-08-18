package com.ecommerce.productcatalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public StockDto(Integer quantity) {
        this.currentStock = quantity;
        this.minimumStock = 0;
    }
    @NotNull(message = "Stok miktarı zorunludur")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer currentStock;

    @Min(value = 0,message = "Stok miktarı negatif olamaz")
    private Integer minimumStock;
}
