package com.ecommerce.productcatalog.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Price {
    private BigDecimal sellingPrice;
    private BigDecimal discountedPrice;
}
