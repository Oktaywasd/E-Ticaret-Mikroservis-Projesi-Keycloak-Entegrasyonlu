package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.request.AdminReplyRequest;
import com.ecommerce.productcatalog.dto.request.QuestionCreateRequest;
import com.ecommerce.productcatalog.dto.request.ReviewCreateRequest;
import com.ecommerce.productcatalog.dto.response.ReviewResponse;
import com.ecommerce.productcatalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Review & Question Management", description = "Ürün yorumları, puanlamalar ve soru-cevap API'leri")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "Ürün Yorumlarını Listele", description = "Belirtilen ürüne ait onaylı müşteri değerlendirmelerini getirir.")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/products/{productId}/questions")
    @Operation(summary = "Ürün Sorularını Listele", description = "Belirtilen ürüne sorulmuş soruları ve admin yanıtlarını getirir.")
    public ResponseEntity<List<ReviewResponse>> getProductQuestions(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getQuestionsByProduct(productId));
    }

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ROLE_CUSTOMER', 'ADMIN', 'ROLE_ADMIN', 'SELLER', 'ROLE_SELLER')")
    @Operation(summary = "Ürüne Puan ve Yorum Ekle", description = "Yalnızca ürünü sipariş etmiş kullanıcılar değerlendirme yapabilir.")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String productId,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaimAsString("preferred_username");
        return new ResponseEntity<>(reviewService.createReview(productId, request, userId, userName), HttpStatus.CREATED);
    }

    @PostMapping("/products/{productId}/questions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ürüne Soru Sor", description = "Giriş yapmış tüm kullanıcılar ürün hakkında soru sorabilir.")
    public ResponseEntity<ReviewResponse> createQuestion(
            @PathVariable String productId,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaimAsString("preferred_username");
        return new ResponseEntity<>(reviewService.createQuestion(productId, request, userId, userName), HttpStatus.CREATED);
    }

    @PostMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'ROLE_ADMIN', 'ROLE_SELLER')")
    @Operation(summary = "Değerlendirme veya Soruya Yanıt Ver", description = "Yöneticiler ve satıcılar yorumlara resmi yanıt ekleyebilir.")
    public ResponseEntity<ReviewResponse> reply(
            @PathVariable String reviewId,
            @Valid @RequestBody AdminReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String adminUsername = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(reviewService.replyToReviewOrQuestion(reviewId, request, adminUsername));
    }
}