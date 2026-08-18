package com.ecommerce.productcatalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;

@AllArgsConstructor //constructer yazmadan lombok bizim için yazar
@Data // bize otomatik metot oluşturur
@NoArgsConstructor // bizim için boş constructer tanımlar
public class PriceDto {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PriceDto(BigDecimal amount) {
        this.sellingPrice = amount;
        this.discountedPrice = null;
    }
    @NotNull(message = "Satış fiyatı zorunludur")
    @Positive(message = "Satış fiyatı 0'dan büyük olamlıdır")
    private BigDecimal sellingPrice;

    @Positive(message = "indirimli fiyat 0'dan büyük olamalıdır")
    private BigDecimal discountedPrice;
}
