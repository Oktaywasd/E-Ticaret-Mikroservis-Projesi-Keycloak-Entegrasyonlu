package com.ecommerce.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private String id;
    private String name;
    private PriceDto price;
    private StockDto stock;

    // --- Inner DTO Classes ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceDto {
        private BigDecimal sellingPrice;
        private BigDecimal discountedPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockDto {
        private Integer currentStock;
        private Integer minimumStock;
    }

    // --- Helper Methods (İş Mantığında Rahat Kullanım İçin) ---

    /**
     * İndirimli fiyat varsa onu, yoksa satış fiyatını döner.
     */
    public BigDecimal getEffectivePrice() {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.getDiscountedPrice() != null ? price.getDiscountedPrice() : price.getSellingPrice();
    }

    /**
     * Mevcut stok miktarını güvenli bir şekilde döner.
     */
    public Integer getCurrentStockCount() {
        return (stock != null && stock.getCurrentStock() != null) ? stock.getCurrentStock() : 0;
    }
}