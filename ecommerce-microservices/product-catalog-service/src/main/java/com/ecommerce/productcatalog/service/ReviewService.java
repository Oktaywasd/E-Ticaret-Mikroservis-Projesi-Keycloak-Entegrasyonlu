package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.AdminReplyRequest;
import com.ecommerce.productcatalog.dto.request.QuestionCreateRequest;
import com.ecommerce.productcatalog.dto.request.ReviewCreateRequest;
import com.ecommerce.productcatalog.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(String productId, ReviewCreateRequest request, String userId, String userName);
    ReviewResponse createQuestion(String productId, QuestionCreateRequest request, String userId, String userName);
    ReviewResponse replyToReviewOrQuestion(String reviewId, AdminReplyRequest request, String adminUsername);
    List<ReviewResponse> getReviewsByProduct(String productId);
    List<ReviewResponse> getQuestionsByProduct(String productId);
}