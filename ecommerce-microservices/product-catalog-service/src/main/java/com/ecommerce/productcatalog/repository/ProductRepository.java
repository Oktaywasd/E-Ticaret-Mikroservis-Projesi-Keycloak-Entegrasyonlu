package com.ecommerce.productcatalog.repository;

import com.ecommerce.productcatalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    boolean existsByProductCodeAndIsDeletedFalse(String productCode);

    Optional<Product> findByIdAndIsDeletedFalse(String id);

    Optional<Product> findByProductCodeAndIsDeletedFalse(String productCode);

    Page<Product> findAllByIsDeletedFalse(Pageable pageable);

    Page<Product> findAllByCategoryIdAndIsDeletedFalse(String categoryId, Pageable pageable);

    List<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(String name, String brand, org.springframework.data.domain.Pageable pageable);

    List<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCaseAndIsDeletedFalse(String name, String brand, org.springframework.data.domain.Pageable pageable);

}