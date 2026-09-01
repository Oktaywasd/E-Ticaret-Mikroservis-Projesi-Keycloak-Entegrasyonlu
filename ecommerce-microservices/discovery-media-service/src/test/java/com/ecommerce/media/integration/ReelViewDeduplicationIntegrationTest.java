package com.ecommerce.media.integration;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.CacheService;
import com.ecommerce.media.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReelViewDeduplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReelRepository reelRepository;

    @MockBean
    private ReelCommentRepository reelCommentRepository;

    @MockBean
    private CacheService cacheService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ProductCatalogClient productCatalogClient;

    @MockBean
    private OrderClient orderClient;

    private final String REEL_ID = "reel_view_test_01";
    private final String USER_ID = "user_viewer_99";
    private Reel sampleReel;

    @BeforeEach
    void setUp() {
        sampleReel = Reel.builder()
                .id(REEL_ID)
                .title("Viral Ürün Tanıtımı")
                .description("İzlenme testi açıklaması")
                .videoUrl("https://minio.test/video.mp4")
                .productId("prod_101")
                .sellerId("seller_202")
                .viewCount(100L)
                .likeCount(20L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("1. Giriş Yapmış Kullanıcı İzlenmesi: viewCount artmalı ve userId set'e eklenmeli")
    void recordView_WhenAuthenticated_ShouldIncrementCountAndAddUserId() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));
        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/reels/{id}/view", REEL_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(reelRepository, times(1)).save(argThat(r ->
                r.getViewCount() == 101L && r.getViewedUserIds().contains(USER_ID)
        ));
    }

    @Test
    @DisplayName("2. Anonim / Public İzlenme: Token olmadan da viewCount artmalı ve 200 OK dönmeli")
    void recordView_WhenAnonymous_ShouldIncrementViewCount() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));
        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/reels/{id}/view", REEL_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(reelRepository, times(1)).save(argThat(r ->
                r.getViewCount() == 101L
        ));
    }

    @Test
    @DisplayName("3. Geçersiz Reel ID: Reel bulunamadığında save çağrılmamalı ve 200 OK dönmeli")
    void recordView_WhenReelNotFound_ShouldReturnOkWithoutSaving() throws Exception {
        when(reelRepository.findById("non_existent_reel")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/reels/{id}/view", "non_existent_reel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(reelRepository, never()).save(any(Reel.class));
    }
}