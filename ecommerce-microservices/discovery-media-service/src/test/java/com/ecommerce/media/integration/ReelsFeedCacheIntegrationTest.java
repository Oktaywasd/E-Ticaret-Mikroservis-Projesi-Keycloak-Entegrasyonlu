package com.ecommerce.media.integration;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.CachedReelFeedResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.CacheService;
import com.ecommerce.media.service.FileStorageService;
import com.ecommerce.media.service.ReelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReelsFeedCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReelService reelService;

    @MockBean
    private CacheService cacheService;

    @MockBean
    private ReelRepository reelRepository;

    @MockBean
    private ReelCommentRepository reelCommentRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ProductCatalogClient productCatalogClient;

    @MockBean
    private OrderClient orderClient;

    private final String REEL_ID = "reel_cache_101";
    private final String PRODUCT_ID = "prod_cache_202";
    private final String SELLER_ID = "seller_cache_303";
    private Reel sampleReel;

    @BeforeEach
    void setUp() {
        sampleReel = Reel.builder()
                .id(REEL_ID)
                .title("Cache Test Video")
                .description("Önbellek test açıklaması")
                .videoUrl("https://minio.test/videos/reel1.mp4")
                .thumbnailUrl("https://minio.test/thumbs/reel1.jpg")
                .durationInSeconds(30)
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .likeCount(15L)
                .viewCount(200L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("1. Cache Miss Senaryosu: Redis'te veri yokken DB'den çekilmeli ve Redis'e yazılmalı")
    void getReelsFeed_WhenCacheMiss_ShouldFetchFromDbAndSetToCache() throws Exception {
        String expectedCacheKey = "cache:reels:feed:page_0_size_10";

        // 1. Redis'te veri yok (Cache Miss)
        when(cacheService.get(expectedCacheKey)).thenReturn(null);

        // 2. MongoDB'den veri dönüyor
        Page<Reel> dbPage = new PageImpl<>(List.of(sampleReel), PageRequest.of(0, 10), 1);
        when(reelRepository.findAllByStatus(eq("ACTIVE"), any(Pageable.class))).thenReturn(dbPage);
        when(reelCommentRepository.countByReelId(REEL_ID)).thenReturn(3L);

        // 3. MockMvc ile feed çağrısı
        mockMvc.perform(get("/api/v1/reels/feed")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(REEL_ID))
                .andExpect(jsonPath("$.content[0].title").value("Cache Test Video"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 4. Doğrulama: DB sorgusu çalıştı ve Redis set() çağrıldı
        verify(reelRepository, times(1)).findAllByStatus(eq("ACTIVE"), any(Pageable.class));
        verify(cacheService, times(1)).set(eq(expectedCacheKey), any(CachedReelFeedResponse.class), anyLong());
    }

    @Test
    @DisplayName("2. Cache Hit Senaryosu: Redis'te veri varken DB sorgusu tetiklenmemeli")
    void getReelsFeed_WhenCacheHit_ShouldReturnDirectlyFromRedisWithoutDbCall() throws Exception {
        String expectedCacheKey = "cache:reels:feed:page_0_size_10";

        ReelResponse cachedReel = ReelResponse.builder()
                .id("cached_reel_999")
                .title("Önbellekten Gelen Video")
                .likeCount(50L)
                .viewCount(1000L)
                .status("ACTIVE")
                .build();

        CachedReelFeedResponse cachedResponse = CachedReelFeedResponse.builder()
                .content(List.of(cachedReel))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1L)
                .totalPages(1)
                .last(true)
                .build();

        // 1. Redis'te hazır veri var (Cache Hit)
        when(cacheService.get(expectedCacheKey)).thenReturn(cachedResponse);

        // 2. MockMvc ile feed çağrısı
        mockMvc.perform(get("/api/v1/reels/feed")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("cached_reel_999"))
                .andExpect(jsonPath("$.content[0].title").value("Önbellekten Gelen Video"));

        // 3. Doğrulama: DB Repository hiç çağrılmadı
        verify(reelRepository, never()).findAllByStatus(anyString(), any(Pageable.class));
        verify(cacheService, never()).set(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("3. Cache Eviction (Yeni Video): Yeni video yüklendiğinde cache:reels:feed:* pattern'i silinmeli")
    void uploadReel_ShouldInvalidateReelsFeedCache() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "reel.mp4", "video/mp4", "video content bytes".getBytes()
        );

        when(fileStorageService.uploadVideo(any())).thenReturn("https://minio.test/videos/new.mp4");
        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> {
            Reel r = invocation.getArgument(0);
            r.setId("new_reel_123");
            return r;
        });

        // Satıcı rolüyle video upload isteği
        mockMvc.perform(multipart("/api/v1/reels/upload")
                        .file(videoFile)
                        .param("title", "Yeni Trend Reels")
                        .param("description", "Açıklama")
                        .param("productId", PRODUCT_ID)
                        .param("durationInSeconds", "15")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                                .jwt(b -> b.subject(SELLER_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("new_reel_123"));

        // Doğrulama: Redis önbelleği pattern bazlı temizlendi mi?
        verify(cacheService, times(1)).deleteByPattern("cache:reels:feed:*");
    }

    @Test
    @DisplayName("4. Cache Eviction (Video Silme): Video silindiğinde feed cache temizlenmeli")
    void deleteReel_ShouldInvalidateReelsFeedCache() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));

        // Sahibi olan satıcı videoyu siliyor
        mockMvc.perform(delete("/api/v1/reels/{id}", REEL_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                                .jwt(b -> b.subject(SELLER_ID))))
                .andExpect(status().isNoContent());

        // Doğrulama: DB'den silindi ve Redis cache temizlendi
        verify(reelRepository, times(1)).delete(eq(sampleReel));
        verify(cacheService, times(1)).deleteByPattern("cache:reels:feed:*");
    }

    @Test
    @DisplayName("5. Cache Eviction (Admin Manuel Temizleme): /admin/cache/clear endpoint'i önbelleği temizlemeli")
    void clearCache_WhenAdminCalls_ShouldInvalidateReelsFeedCache() throws Exception {
        mockMvc.perform(delete("/api/v1/reels/admin/cache/clear")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(b -> b.subject("admin_user"))))
                .andExpect(status().isNoContent());

        verify(cacheService, times(1)).deleteByPattern("cache:reels:feed:*");
    }
}
