package com.ecommerce.productcatalog.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Silinecek Redis önbellek anahtar deseni (örn: "cache:top_*", "products::*", "categories::*")
     */
    private String cachePattern;

    /**
     * Önbellek temizliğini tetikleyen operasyon kaynağı (örn: "STOCK_UPDATE", "PRODUCT_EDIT", "PRODUCT_DELETE")
     */
    private String triggeredBy;
}