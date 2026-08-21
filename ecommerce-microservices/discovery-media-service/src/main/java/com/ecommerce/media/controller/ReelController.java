package com.ecommerce.media.controller;

import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.service.ReelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reels")
@RequiredArgsConstructor
@Tag(name = "Reels Controller", description = "Reels Discovery & Social Media Endpoints")
public class ReelController {

    private final ReelService reelService;

    @GetMapping("/feed")
    @Operation(summary = "Get paginated reels feed (Public)")
    public ResponseEntity<Page<ReelResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reelService.getReelsFeed(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single reel details (Public)")
    public ResponseEntity<ReelResponse> getReelById(@PathVariable String id) {
        return ResponseEntity.ok(reelService.getReelById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload a new reel video (Seller & Admin)")
    public ResponseEntity<ReelResponse> uploadReel(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("productId") String productId,
            @RequestParam(value = "durationInSeconds", required = false, defaultValue = "30") Integer durationInSeconds,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @AuthenticationPrincipal Jwt jwt) {

        CreateReelRequest request = new CreateReelRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setProductId(productId);
        request.setDurationInSeconds(durationInSeconds != null ? durationInSeconds : 30);

        String sellerId = (jwt != null) ? jwt.getSubject() : "anonymous-seller";
        ReelResponse response = reelService.uploadReel(request, videoFile, thumbnailFile, sellerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Increment video view count atomically (Public)")
    public ResponseEntity<Void> incrementView(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null) ? jwt.getSubject() : null;
        reelService.incrementViewCount(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Like a reel video (Customer)")
    public ResponseEntity<Void> likeReel(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous-user";
        reelService.toggleLikeReel(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{reelId}/comments")
    @Operation(summary = "Get comments of a reel (Public)")
    public ResponseEntity<List<ReelCommentResponse>> getComments(@PathVariable String reelId) {
        return ResponseEntity.ok(reelService.getComments(reelId));
    }

    @PostMapping("/{reelId}/comments")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add comment to a reel with verified buyer check (Customer)")
    public ResponseEntity<ReelCommentResponse> addComment(
            @PathVariable String reelId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = (jwt != null) ? jwt.getSubject() : "anonymous-user";
        String fullName = extractFullName(jwt);
        return new ResponseEntity<>(reelService.addComment(reelId, request, userId, fullName), HttpStatus.CREATED);
    }

    @PostMapping("/comments/{id}/pin")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Pin/Unpin comment to top (Seller & Admin)")
    public ResponseEntity<Void> pinComment(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous-user";
        reelService.pinComment(id, userId, hasAdminRole(jwt));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete reel (Seller & Admin)")
    public ResponseEntity<Void> deleteReel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous-user";
        reelService.deleteReel(id, userId, hasAdminRole(jwt));
        return ResponseEntity.noContent().build();
    }

    private String extractFullName(Jwt jwt) {
        if (jwt == null) {
            return "Anonim Kullanıcı";
        }
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        if ((givenName != null && !givenName.isBlank()) || (familyName != null && !familyName.isBlank())) {
            return String.format("%s %s", givenName != null ? givenName : "", familyName != null ? familyName : "").trim();
        }

        String username = jwt.getClaimAsString("preferred_username");
        return (username != null && !username.isBlank()) ? username : "Kullanıcı";
    }

    @SuppressWarnings("unchecked")
    private boolean hasAdminRole(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
        }
        return false;
    }
}