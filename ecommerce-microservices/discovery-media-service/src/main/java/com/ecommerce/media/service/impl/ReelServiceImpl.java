package com.ecommerce.media.service.impl;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.CachedReelFeedResponse;
import com.ecommerce.media.dto.response.ProductSummaryResponse;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.exception.ResourceNotFoundException;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.CacheService;
import com.ecommerce.media.service.FileStorageService;
import com.ecommerce.media.service.ReelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final ReelCommentRepository commentRepository;
    private final FileStorageService fileStorageService;
    private final ProductCatalogClient productCatalogClient;
    private final OrderClient orderClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String CACHE_REELS_FEED_PREFIX = "cache:reels:feed:";

    @Value("${app.cache.reels-feed-ttl:1800}")
    private long reelsFeedTtl;

    @Override
    public ReelResponse uploadReel(CreateReelRequest request, MultipartFile videoFile, MultipartFile thumbnailFile, String sellerId) {
        ProductSummaryResponse productSummary = null;
        try {
            productSummary = productCatalogClient.getProductSummaryById(request.getProductId());
        } catch (Exception e) {
            log.warn("Ürün bilgisi catalog servisinden alınamadı: {}", e.getMessage());
        }

        String videoUrl = fileStorageService.uploadVideo(videoFile);
        String thumbnailUrl = (thumbnailFile != null && !thumbnailFile.isEmpty())
                ? fileStorageService.uploadThumbnail(thumbnailFile)
                : (productSummary != null ? productSummary.getThumbnailUrl() : null);

        Reel reel = Reel.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .durationInSeconds(request.getDurationInSeconds())
                .productId(request.getProductId())
                .sellerId(sellerId)
                .likeCount(0L)
                .viewCount(0L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();

        Reel savedReel = reelRepository.save(reel);
        clearReelsFeedCache(); // Yeni video yüklendiğinde feed cache temizlenir
        return mapToReelResponse(savedReel, productSummary, 0L);
    }

    @Override
    public Page<ReelResponse> getReelsFeed(Pageable pageable) {
        String cacheKey = CACHE_REELS_FEED_PREFIX + "page_" + pageable.getPageNumber() + "_size_" + pageable.getPageSize();

        Object cachedData = cacheService.get(cacheKey);
        if (cachedData != null) {
            try {
                CachedReelFeedResponse cachedFeed = objectMapper.convertValue(cachedData, CachedReelFeedResponse.class);
                return new PageImpl<>(cachedFeed.getContent(), pageable, cachedFeed.getTotalElements());
            } catch (Exception e) {
                log.warn("Reels feed cache deserialize edilemedi, DB'den çekiliyor: {}", e.getMessage());
            }
        }

        Page<ReelResponse> feedPage = fetchReelsFeedFromDb(pageable);

        CachedReelFeedResponse toCache = CachedReelFeedResponse.builder()
                .content(feedPage.getContent())
                .pageNumber(feedPage.getNumber())
                .pageSize(feedPage.getSize())
                .totalElements(feedPage.getTotalElements())
                .totalPages(feedPage.getTotalPages())
                .last(feedPage.isLast())
                .build();

        cacheService.set(cacheKey, toCache, reelsFeedTtl);
        return feedPage;
    }

    @Override
    public Page<ReelResponse> fetchReelsFeedFromDb(Pageable pageable) {
        return reelRepository.findAllByStatus("ACTIVE", pageable)
                .map(reel -> {
                    ProductSummaryResponse summary = fetchProductSummarySafe(reel.getProductId());
                    long commentCount = commentRepository.countByReelId(reel.getId());
                    return mapToReelResponse(reel, summary, commentCount);
                });
    }

    @Override
    public ReelResponse getReelById(String id) {
        Reel reel = reelRepository.findByIdAndStatus(id, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + id));
        long commentCount = commentRepository.countByReelId(reel.getId());
        return mapToReelResponse(reel, fetchProductSummarySafe(reel.getProductId()), commentCount);
    }

    @Override
    public void incrementViewCount(String id, String userId) {
        Reel reel = reelRepository.findById(id).orElse(null);
        if (reel == null) {
            return;
        }

        reel.setViewCount((reel.getViewCount() != null ? reel.getViewCount() : 0) + 1);

        if (userId != null && !userId.isBlank()) {
            if (reel.getViewedUserIds() == null) {
                reel.setViewedUserIds(new HashSet<>());
            }
            reel.getViewedUserIds().add(userId);
        }

        reelRepository.save(reel);
    }

    @Override
    public void toggleLikeReel(String id, String userId) {
        Reel reel = reelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + id));

        if (reel.getLikedUserIds() == null) {
            reel.setLikedUserIds(new HashSet<>());
        }

        if (reel.getLikedUserIds().contains(userId)) {
            reel.getLikedUserIds().remove(userId);
            reel.setLikeCount(Math.max(0, (reel.getLikeCount() != null ? reel.getLikeCount() : 1) - 1));
        } else {
            reel.getLikedUserIds().add(userId);
            reel.setLikeCount((reel.getLikeCount() != null ? reel.getLikeCount() : 0) + 1);
        }

        reelRepository.save(reel);
    }

    @Override
    public ReelCommentResponse addComment(String reelId, CreateCommentRequest request, String userId, String username) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel bulunamadı: " + reelId));

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
                .likeCount(0L)
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
        clearReelsFeedCache(); // Silindiğinde feed cache temizlenir
    }

    @Override
    public void clearReelsFeedCache() {
        cacheService.deleteByPattern(CACHE_REELS_FEED_PREFIX + "*");
        log.info("Tüm Reels feed sayfaları cache'ten silindi.");
    }

    private ProductSummaryResponse fetchProductSummarySafe(String productId) {
        try {
            return productCatalogClient.getProductSummaryById(productId);
        } catch (Exception e) {
            return null;
        }
    }

    private ReelResponse mapToReelResponse(Reel reel, ProductSummaryResponse product, long commentCount) {
        Set<String> finalLikedUsers = new HashSet<>();
        if (reel.getLikedUserIds() != null) {
            finalLikedUsers.addAll(reel.getLikedUserIds());
        }

        Set<String> finalViewedUsers = new HashSet<>();
        if (reel.getViewedUserIds() != null) {
            finalViewedUsers.addAll(reel.getViewedUserIds());
        }

        return ReelResponse.builder()
                .id(reel.getId())
                .title(reel.getTitle())
                .description(reel.getDescription())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .durationInSeconds(reel.getDurationInSeconds())
                .productId(reel.getProductId())
                .sellerId(reel.getSellerId())
                .likeCount(reel.getLikeCount() != null ? reel.getLikeCount() : 0)
                .viewCount(reel.getViewCount() != null ? reel.getViewCount() : 0)
                .likedUserIds(finalLikedUsers)
                .viewedUserIds(finalViewedUsers)
                .commentCount(commentCount)
                .status(reel.getStatus())
                .createdAt(reel.getCreatedAt())
                .productName(product != null ? product.getName() : null)
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
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}