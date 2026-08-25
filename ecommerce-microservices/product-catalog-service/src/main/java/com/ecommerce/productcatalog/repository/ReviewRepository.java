package com.ecommerce.productcatalog.repository;

import com.ecommerce.productcatalog.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductIdAndTypeOrderByCreatedAtDesc(String productId, String type);
    long countByProductIdAndType(String productId, String type);
}