package com.ecommerce.media.controller;

import com.ecommerce.media.config.KeycloakJwtAuthenticationConverter;
import com.ecommerce.media.config.SecurityConfig;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.service.ReelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReelController.class)
@Import(SecurityConfig.class)
class ReelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReelService reelService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final String REEL_ID = "reel_101";
    private final String SELLER_ID = "seller_uuid_123";
    private final String CUSTOMER_ID = "customer_uuid_456";

    private ReelResponse reelResponse;

    @BeforeEach
    void setUp() {
        reelResponse = ReelResponse.builder()
                .id(REEL_ID)
                .title("Yeni Sezon Sneaker İncelemesi")
                .description("Rahat ve şık spor ayakkabı")
                .productId("prod_101")
                .sellerId(SELLER_ID)
                .videoUrl("http://minio/reels/video.mp4")
                .likeCount(15L)
                .viewCount(250L)
                .build();
    }

    @Test
    @DisplayName("Public Endpoint: Anonim kullanıcı reels akışını (Feed) 200 OK ile alabilmeli")
    void getFeed_WhenAnonymousUser_ShouldReturn200Ok() throws Exception {
        when(reelService.getReelsFeed(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reelResponse)));

        mockMvc.perform(get("/api/v1/reels/feed")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(REEL_ID));

        verify(reelService, times(1)).getReelsFeed(any(Pageable.class));
    }

    @Test
    @DisplayName("Public Endpoint: Video izlenme sayısı token olmadan 200 OK ile artırılabilmeli")
    void incrementView_WhenAnonymousUser_ShouldReturn200Ok() throws Exception {
        doNothing().when(reelService).incrementViewCount(eq(REEL_ID), any());

        mockMvc.perform(post("/api/v1/reels/" + REEL_ID + "/view"))
                .andExpect(status().isOk());

        verify(reelService, times(1)).incrementViewCount(eq(REEL_ID), isNull());
    }

    @Test
    @DisplayName("RBAC Yetkisiz: Normal müşteri (ROLE_CUSTOMER) video yüklemeye çalışırsa 403 Forbidden dönmeli")
    void uploadReel_WhenCustomerRole_ShouldReturn403Forbidden() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "sample.mp4", "video/mp4", "fake-video-content".getBytes());

        mockMvc.perform(multipart("/api/v1/reels/upload")
                        .file(videoFile)
                        .param("title", "Yeni Ürün")
                        .param("description", "Açıklama")
                        .param("productId", "prod_101")
                        .with(jwt().jwt(jwt -> jwt.subject(CUSTOMER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        verify(reelService, never()).uploadReel(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Başarılı Video Yükleme: Satıcı (ROLE_SELLER) multipart video yükleyebilmeli ve 201 Created dönmeli")
    void uploadReel_WhenSellerRole_ShouldReturn201Created() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "sample.mp4", "video/mp4", "fake-video-content".getBytes());

        when(reelService.uploadReel(any(), any(), any(), eq(SELLER_ID)))
                .thenReturn(reelResponse);

        mockMvc.perform(multipart("/api/v1/reels/upload")
                        .file(videoFile)
                        .param("title", "Yeni Sezon Sneaker İncelemesi")
                        .param("description", "Rahat ve şık spor ayakkabı")
                        .param("productId", "prod_101")
                        .param("durationInSeconds", "30")
                        .with(jwt().jwt(jwt -> jwt.subject(SELLER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REEL_ID))
                .andExpect(jsonPath("$.title").value("Yeni Sezon Sneaker İncelemesi"));

        verify(reelService, times(1)).uploadReel(any(), any(), any(), eq(SELLER_ID));
    }

    @Test
    @DisplayName("Sosyal Etkileşim: Müşteri (ROLE_CUSTOMER) videoyu beğenebilmeli ve 200 OK dönmeli")
    void likeReel_WhenCustomerRole_ShouldReturn200Ok() throws Exception {
        doNothing().when(reelService).toggleLikeReel(REEL_ID, CUSTOMER_ID);

        mockMvc.perform(post("/api/v1/reels/" + REEL_ID + "/like")
                        .with(jwt().jwt(jwt -> jwt.subject(CUSTOMER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());

        verify(reelService, times(1)).toggleLikeReel(REEL_ID, CUSTOMER_ID);
    }

    @Test
    @DisplayName("Yorum Ekleme: Müşteri (ROLE_CUSTOMER) geçerli yorumla 201 Created almalı")
    void addComment_WhenValidRequest_ShouldReturn201Created() throws Exception {
        CreateCommentRequest commentRequest = CreateCommentRequest.builder()
                .content("Ürünü satın aldım, kumaşı harika!")
                .build();

        ReelCommentResponse commentResponse = ReelCommentResponse.builder()
                .id("comm_001")
                .reelId(REEL_ID)
                .userId(CUSTOMER_ID)
                .username("Ahmet Yılmaz")
                .content("Ürünü satın aldım, kumaşı harika!")
                .isVerifiedBuyer(true)
                .isPinned(false)
                .likeCount(0L)
                .createdAt(Instant.now())
                .build();

        when(reelService.addComment(eq(REEL_ID), any(CreateCommentRequest.class), eq(CUSTOMER_ID), eq("Ahmet Yılmaz")))
                .thenReturn(commentResponse);

        mockMvc.perform(post("/api/v1/reels/" + REEL_ID + "/comments")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject(CUSTOMER_ID);
                            jwt.claim("name", "Ahmet Yılmaz");
                        }).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("comm_001"))
                .andExpect(jsonPath("$.content").value("Ürünü satın aldım, kumaşı harika!"))
                .andExpect(jsonPath("$.isVerifiedBuyer").value(true));

        verify(reelService, times(1)).addComment(eq(REEL_ID), any(CreateCommentRequest.class), eq(CUSTOMER_ID), eq("Ahmet Yılmaz"));
    }

    @Test
    @DisplayName("Yorum Başa Sabitleme: Satıcı/Admin başa sabitleyebilmeli ve 200 OK dönmeli")
    void pinComment_WhenSellerRole_ShouldReturn200Ok() throws Exception {
        doNothing().when(reelService).pinComment(eq("comm_001"), eq(SELLER_ID), anyBoolean());

        mockMvc.perform(post("/api/v1/reels/comments/comm_001/pin")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject(SELLER_ID);
                            jwt.claim("realm_access", Map.of("roles", List.of("SELLER")));
                        }).authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isOk());

        verify(reelService, times(1)).pinComment(eq("comm_001"), eq(SELLER_ID), eq(false));
    }

    @Test
    @DisplayName("Reel Silme: Admin videoyu 204 No Content ile silebilmeli")
    void deleteReel_WhenAdminRole_ShouldReturn204NoContent() throws Exception {
        doNothing().when(reelService).deleteReel(eq(REEL_ID), eq(SELLER_ID), anyBoolean());

        mockMvc.perform(delete("/api/v1/reels/" + REEL_ID)
                        .with(jwt().jwt(jwt -> {
                            jwt.subject(SELLER_ID);
                            jwt.claim("realm_access", Map.of("roles", List.of("ADMIN")));
                        }).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(reelService, times(1)).deleteReel(eq(REEL_ID), eq(SELLER_ID), eq(true));
    }
}