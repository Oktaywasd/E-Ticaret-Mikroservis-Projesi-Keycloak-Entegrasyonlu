package com.ecommerce.productcatalog.repository;

import com.ecommerce.productcatalog.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    // Kategori adının benzersiz olup olmadığını kontrol etmek için
    boolean existsByNameIgnoreCase(String name);
}
