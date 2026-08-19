package com.ecommerce.media.service.impl;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.ProductSummaryResponse;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.exception.ResourceNotFoundException;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.FileStorageService;
import com.ecommerce.media.service.ReelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final ReelCommentRepository commentRepository;
    private final FileStorageService fileStorageService;
    private final ProductCatalogClient productCatalogClient;
    private final OrderClient orderClient;
    private final MongoTemplate mongoTemplate;

    @Override
    public ReelResponse uploadReel(CreateReelRequest request, MultipartFile videoFile, MultipartFile thumbnailFile, String sellerId) {
        // 1. Catalog servisinden ürün doğrulama ve özet bilgi alma
        ProductSummaryResponse productSummary = null;
        try {
            productSummary = productCatalogClient.getProductSummaryById(request.getProductId());
        } catch (Exception e) {
            log.warn("Ürün bilgisi catalog servisinden alınamadı: {}", e.getMessage());
        }

        // 2. MinIO'ya video ve thumbnail yükleme
        String videoUrl = fileStorageService.uploadVideo(videoFile);
        String thumbnailUrl = (thumbnailFile != null && !thumbnailFile.isEmpty())
                ? fileStorageService.uploadThumbnail(thumbnailFile)
                : (productSummary != null ? productSummary.getThumbnailUrl() : null);

        // 3. Reel dokümanını kaydetme[cite: 2]
        Reel reel = Reel.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .durationInSeconds(request.getDurationInSeconds())
                .productId(request.getProductId())
                .sellerId(sellerId)
                .build();

        Reel savedReel = reelRepository.save(reel);
        return mapToReelResponse(savedReel, productSummary);
    }

    @Override
    public Page<ReelResponse> getReelsFeed(Pageable pageable) {
        return reelRepository.findAllByStatus("ACTIVE", pageable)
                .map(reel -> {
                    ProductSummaryResponse summary = fetchProductSummarySafe(reel.getProductId());
                    return mapToReelResponse(reel, summary);
                });
    }

    @Override
    public ReelResponse getReelById(String id) {
        Reel reel = reelRepository.findByIdAndStatus(id, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + id));
        return mapToReelResponse(reel, fetchProductSummarySafe(reel.getProductId()));
    }

    @Override
    public void incrementViewCount(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Reel.class);
    }

    @Override
    public void toggleLikeReel(String id, String userId) {
        // İlerleyen aşamada kullanıcı bazlı like koleksiyonu tutulabilir; şu an atomik +1 artırıyoruz[cite: 2]
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().inc("likeCount", 1);
        mongoTemplate.updateFirst(query, update, Reel.class);
    }

    @Override
    public ReelCommentResponse addComment(String reelId, CreateCommentRequest request, String userId, String username) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + reelId));

        // Order Service üzerinden mavi tik kontrolü[cite: 2]
        Boolean isVerified = false;
        try {
            isVerified = orderClient.verifyPurchase(userId, reel.getProductId());
        } catch (Exception e) {
            log.warn("Sipariş doğrulaması yapılamadı: {}", e.getMessage());
        }

        ReelComment comment = ReelComment.builder()
                .reelId(reelId)
                .userId(userId)
                .username(username)
                .content(request.getContent())
                .isVerifiedBuyer(Boolean.TRUE.equals(isVerified))
                .isPinned(false)
                .build();

        ReelComment savedComment = commentRepository.save(comment);
        return mapToCommentResponse(savedComment);
    }

    @Override
    public List<ReelCommentResponse> getComments(String reelId) {
        return commentRepository.findAllByReelIdOrderByIsPinnedDescCreatedAtDesc(reelId)
                .stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    @Override
    public void pinComment(String commentId, String currentUserId, boolean isAdmin) {
        ReelComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: " + commentId));

        Reel reel = reelRepository.findById(comment.getReelId())
                .orElseThrow(() -> new ResourceNotFoundException("İlgili reel bulunamadı"));

        // Yalnızca admin veya videonun satıcısı sabitleyebilir[cite: 2]
        if (!isAdmin && !reel.getSellerId().equals(currentUserId)) {
            throw new AccessDeniedException("Bu yorumu sabitleme yetkiniz bulunmamaktadır.");
        }

        comment.setIsPinned(!Boolean.TRUE.equals(comment.getIsPinned()));
        commentRepository.save(comment);
    }

    @Override
    public void deleteReel(String id, String currentUserId, boolean isAdmin) {
        Reel reel = reelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + id));

        if (!isAdmin && !reel.getSellerId().equals(currentUserId)) {
            throw new AccessDeniedException("Bu videoyu silme yetkiniz bulunmamaktadır.");
        }

        commentRepository.deleteAllByReelId(id);
        reelRepository.delete(reel);
    }

    private ProductSummaryResponse fetchProductSummarySafe(String productId) {
        try {
            return productCatalogClient.getProductSummaryById(productId);
        } catch (Exception e) {
            return null;
        }
    }

    private ReelResponse mapToReelResponse(Reel reel, ProductSummaryResponse product) {
        return ReelResponse.builder()
                .id(reel.getId())
                .title(reel.getTitle())
                .description(reel.getDescription())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .durationInSeconds(reel.getDurationInSeconds())
                .productId(reel.getProductId())
                .sellerId(reel.getSellerId())
                .likeCount(reel.getLikeCount())
                .viewCount(reel.getViewCount())
                .status(reel.getStatus())
                .createdAt(reel.getCreatedAt())
                .product(product)
                .build();
    }

    private ReelCommentResponse mapToCommentResponse(ReelComment comment) {
        return ReelCommentResponse.builder()
                .id(comment.getId())
                .reelId(comment.getReelId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .content(comment.getContent())
                .isVerifiedBuyer(comment.getIsVerifiedBuyer())
                .isPinned(comment.getIsPinned())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}