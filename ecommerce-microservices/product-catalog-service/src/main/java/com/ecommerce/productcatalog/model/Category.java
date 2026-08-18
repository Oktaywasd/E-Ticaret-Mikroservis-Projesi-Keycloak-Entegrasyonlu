package com.ecommerce.productcatalog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "categories")
public class Category {

    @Id
    private String id;
    private String name;
    private String parantCategoryId;
}
