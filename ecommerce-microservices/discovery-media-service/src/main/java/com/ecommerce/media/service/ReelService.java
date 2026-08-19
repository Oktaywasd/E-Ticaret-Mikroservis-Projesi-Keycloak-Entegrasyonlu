package com.ecommerce.media.service;

import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReelService {

    ReelResponse uploadReel(CreateReelRequest request, MultipartFile videoFile, MultipartFile thumbnailFile, String sellerId);

    Page<ReelResponse> getReelsFeed(Pageable pageable);

    ReelResponse getReelById(String id);

    void incrementViewCount(String id);

    void toggleLikeReel(String id, String userId);

    ReelCommentResponse addComment(String reelId, CreateCommentRequest request, String userId, String username);

    List<ReelCommentResponse> getComments(String reelId);

    void pinComment(String commentId, String currentUserId, boolean isAdmin);

    void deleteReel(String id, String currentUserId, boolean isAdmin);
}