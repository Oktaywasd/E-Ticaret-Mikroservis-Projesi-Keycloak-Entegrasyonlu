package com.ecommerce.media.repository;

import com.ecommerce.media.model.ReelComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReelCommentRepository extends MongoRepository<ReelComment, String> {
    List<ReelComment> findAllByReelIdOrderByIsPinnedDescCreatedAtDesc(String reelId);
    Page<ReelComment> findAllByReelIdOrderByIsPinnedDescCreatedAtDesc(String reelId, Pageable pageable);
    long countByReelId(String reelId);
    void deleteAllByReelId(String reelId);
}