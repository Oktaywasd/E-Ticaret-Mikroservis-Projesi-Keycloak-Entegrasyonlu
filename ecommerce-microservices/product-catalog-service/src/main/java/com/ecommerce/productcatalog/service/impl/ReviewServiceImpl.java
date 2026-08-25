package com.ecommerce.productcatalog.service.impl;

import com.ecommerce.productcatalog.client.OrderClient;
import com.ecommerce.productcatalog.dto.request.AdminReplyRequest;
import com.ecommerce.productcatalog.dto.request.QuestionCreateRequest;
import com.ecommerce.productcatalog.dto.request.ReviewCreateRequest;
import com.ecommerce.productcatalog.dto.response.ReviewResponse;
import com.ecommerce.productcatalog.exception.BusinessException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Review;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.repository.ReviewRepository;
import com.ecommerce.productcatalog.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public ReviewResponse createReview(String productId, ReviewCreateRequest request, String userId, String userName) {
        Product product = findActiveProduct(productId);

        // 1. Sipariş Doğrulaması (Feign ile OrderService kontrolü)
        Boolean hasPurchased = false;
        try {
            hasPurchased = orderClient.verifyPurchase(userId, productId);
        } catch (Exception e) {
            log.error("OrderService ile iletişim kurulamadı: ", e);
            throw new BusinessException("Sipariş geçmişi doğrulanırken bir hata oluştu.");
        }

        if (Boolean.FALSE.equals(hasPurchased)) {
            throw new BusinessException("Yalnızca ürünü sipariş etmiş olan kullanıcılar değerlendirme yapabilir.");
        }

        // 2. Review Kaydı Oluşturma
        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .userName(userName != null ? userName : "Anonim Kullanıcı")
                .type("REVIEW")
                .rating(request.getRating())
                .comment(request.getComment())
                .isVerifiedBuyer(true)
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // 3. Ürün Puan Ortalaması ve Değerlendirme Sayısını Güncelleme
        updateProductRating(product, request.getRating());

        return toReviewResponse(savedReview);
    }

    @Override
    public ReviewResponse createQuestion(String productId, QuestionCreateRequest request, String userId, String userName) {
        findActiveProduct(productId); // Ürün var mı kontrolü

        Review question = Review.builder()
                .productId(productId)
                .userId(userId)
                .userName(userName != null ? userName : "Kullanıcı")
                .type("QUESTION")
                .rating(null)
                .comment(request.getQuestion())
                .isVerifiedBuyer(false)
                .createdAt(LocalDateTime.now())
                .build();

        Review savedQuestion = reviewRepository.save(question);
        return toReviewResponse(savedQuestion);
    }

    @Override
    public ReviewResponse replyToReviewOrQuestion(String reviewId, AdminReplyRequest request, String adminUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Değerlendirme/Soru bulunamadı ID: " + reviewId));

        Review.AdminReply adminReply = Review.AdminReply.builder()
                .replyText(request.getReplyText())
                .repliedBy(adminUsername != null ? adminUsername : "Satıcı/Yönetici")
                .repliedAt(LocalDateTime.now())
                .build();

        review.setAdminReply(adminReply);
        Review updatedReview = reviewRepository.save(review);

        return toReviewResponse(updatedReview);
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(String productId) {
        return reviewRepository.findByProductIdAndTypeOrderByCreatedAtDesc(productId, "REVIEW")
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getQuestionsByProduct(String productId) {
        return reviewRepository.findByProductIdAndTypeOrderByCreatedAtDesc(productId, "QUESTION")
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    // Ürün Ortalama Puanını Dinamik Güncelleyen Metod
    private void updateProductRating(Product product, Integer newRating) {
        int oldCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        double oldAvg = product.getRatingAverage() != null ? product.getRatingAverage() : 0.0;

        int newCount = oldCount + 1;
        double newAvg = ((oldAvg * oldCount) + newRating) / newCount;

        // 1 ondalık basamağa yuvarla (Örn: 4.5)
        newAvg = Math.round(newAvg * 10.0) / 10.0;

        product.setReviewCount(newCount);
        product.setRatingAverage(newAvg);
        product.setUpdatedDate(LocalDateTime.now());

        productRepository.save(product);
        log.info("Ürün puanı güncellendi. ProductId: {}, Yeni Ortalama: {}, Toplam Değerlendirme: {}",
                product.getId(), newAvg, newCount);
    }

    private Product findActiveProduct(String productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı ID: " + productId));
    }

    // Entity -> DTO Manuel Eşleme (İstenirse MapStruct'a da taşınabilir)
    private ReviewResponse toReviewResponse(Review review) {
        ReviewResponse.AdminReplyDto replyDto = null;
        if (review.getAdminReply() != null) {
            replyDto = ReviewResponse.AdminReplyDto.builder()
                    .replyText(review.getAdminReply().getReplyText())
                    .repliedBy(review.getAdminReply().getRepliedBy())
                    .repliedAt(review.getAdminReply().getRepliedAt())
                    .build();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .type(review.getType())
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedBuyer(review.isVerifiedBuyer())
                .adminReply(replyDto)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
