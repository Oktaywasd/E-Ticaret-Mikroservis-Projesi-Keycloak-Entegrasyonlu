package com.ecommerce.media.repository;

import com.ecommerce.media.model.Reel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReelRepository extends MongoRepository<Reel, String> {
    Page<Reel> findAllByStatus(String status, Pageable pageable);
    Optional<Reel> findByIdAndStatus(String id, String status);
}