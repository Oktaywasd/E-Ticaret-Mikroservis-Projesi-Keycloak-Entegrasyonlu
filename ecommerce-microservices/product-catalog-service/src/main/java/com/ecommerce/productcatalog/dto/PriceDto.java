package com.ecommerce.productcatalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDto {

    @NotNull(message = "Satış fiyatı zorunludur")
    @Positive(message = "Satış fiyatı 0'dan büyük olmalıdır")
    private BigDecimal sellingPrice;

    @Positive(message = "İndirimli fiyat 0'dan büyük olmalıdır")
    private BigDecimal discountedPrice;
}