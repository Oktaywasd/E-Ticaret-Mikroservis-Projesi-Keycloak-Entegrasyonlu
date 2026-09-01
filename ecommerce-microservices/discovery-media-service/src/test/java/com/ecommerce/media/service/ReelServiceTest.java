package com.ecommerce.media.service;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.response.CachedReelFeedResponse;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.impl.ReelServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReelServiceTest {

    @Mock
    private ReelRepository reelRepository;

    @Mock
    private ReelCommentRepository commentRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    private ReelServiceImpl reelService;

    private Reel reel;
    private ReelResponse reelResponse;
    private final String REEL_ID = "reel_101";
    private final String USER_ID = "user_123";
    private final String SELLER_ID = "seller_999";
    private final String PRODUCT_ID = "prod_001";

    @BeforeEach
    void setUp() {
        // FileStorageService mock'lanmadan dummy nesne olarak iletilir (Java 23 Mockito proxy hatasını önler)
        FileStorageService dummyFileStorageService = new FileStorageService(null, null) {
            @Override
            public String uploadVideo(MultipartFile file) {
                return "https://minio.local/videos/test.mp4";
            }

            @Override
            public String uploadThumbnail(MultipartFile file) {
                return "https://minio.local/thumbnails/test.jpg";
            }
        };

        reelService = new ReelServiceImpl(
                reelRepository,
                commentRepository,
                dummyFileStorageService,
                productCatalogClient,
                orderClient,
                cacheService,
                objectMapper
        );

        ReflectionTestUtils.setField(reelService, "reelsFeedTtl", 1800L);

        reel = Reel.builder()
                .id(REEL_ID)
                .title("Harika Ürün Tanıtımı")
                .description("Bu tişörtü mutlaka deneyin!")
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .videoUrl("https://minio.local/videos/reel101.mp4")
                .likeCount(0L)
                .viewCount(0L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();

        reelResponse = ReelResponse.builder()
                .id(REEL_ID)
                .title("Harika Ürün Tanıtımı")
                .description("Bu tişörtü mutlaka deneyin!")
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .videoUrl("https://minio.local/videos/reel101.mp4")
                .build();
    }

    @Test
    @DisplayName("İzlenme Artışı: incrementViewCount çağrıldığında viewCount artmalı ve kullanıcı listeye eklenmeli")
    void incrementViewCount_WhenCalled_ShouldIncreaseViewCountAndAddUser() {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        reelService.incrementViewCount(REEL_ID, USER_ID);

        assertThat(reel.getViewCount()).isEqualTo(1L);
        assertThat(reel.getViewedUserIds()).contains(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Beğeni Ekleme: İlk kez beğenen kullanıcının beğenisi likeCount'ı artırmalı")
    void toggleLikeReel_WhenNotLikedBefore_ShouldAddLike() {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        reelService.toggleLikeReel(REEL_ID, USER_ID);

        assertThat(reel.getLikeCount()).isEqualTo(1L);
        assertThat(reel.getLikedUserIds()).contains(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Beğeni Geri Çekme: Zaten beğenmiş kullanıcı tekrar basarsa beğeni geri alınmalı")
    void toggleLikeReel_WhenAlreadyLiked_ShouldRemoveLike() {
        reel.setLikeCount(1L);
        reel.getLikedUserIds().add(USER_ID);
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        reelService.toggleLikeReel(REEL_ID, USER_ID);

        assertThat(reel.getLikeCount()).isEqualTo(0L);
        assertThat(reel.getLikedUserIds()).doesNotContain(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Reels Mavi Tik: Ürünü satın almış kullanıcı yorum yaptığında isVerifiedBuyer true olmalı")
    void addComment_WhenUserPurchasedProduct_ShouldSetVerifiedBuyerTrue() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Kumaşı gerçekten çok kaliteli!");

        ReelComment comment = ReelComment.builder()
                .id("comm_1")
                .reelId(REEL_ID)
                .userId(USER_ID)
                .username("ahmet")
                .content(request.getContent())
                .isVerifiedBuyer(true)
                .isPinned(false)
                .likeCount(0L)
                .build();

        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(orderClient.verifyPurchase(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(commentRepository.save(any(ReelComment.class))).thenReturn(comment);

        ReelCommentResponse response = reelService.addComment(REEL_ID, request, USER_ID, "ahmet");

        assertThat(response).isNotNull();
        assertThat(response.getIsVerifiedBuyer()).isTrue();
        verify(orderClient, times(1)).verifyPurchase(USER_ID, PRODUCT_ID);
        verify(commentRepository, times(1)).save(any(ReelComment.class));
    }

    @Test
    @DisplayName("Yorum Sabitleme Yetkisi: Satıcı kendi videosundaki yorumu sabitleyebilmeli")
    void pinComment_WhenSeller_ShouldTogglePin() {
        ReelComment comment = ReelComment.builder()
                .id("comm_1")
                .reelId(REEL_ID)
                .isPinned(false)
                .build();

        when(commentRepository.findById("comm_1")).thenReturn(Optional.of(comment));
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(commentRepository.save(any(ReelComment.class))).thenReturn(comment);

        reelService.pinComment("comm_1", SELLER_ID, false);

        assertThat(comment.getIsPinned()).isTrue();
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    @DisplayName("Yorum Sabitleme Yetkisiz Erişim: Başka bir kullanıcı yorum sabitlemeye çalışırsa AccessDeniedException fırlatmalı")
    void pinComment_WhenNotSellerAndNotAdmin_ShouldThrowAccessDeniedException() {
        ReelComment comment = ReelComment.builder()
                .id("comm_1")
                .reelId(REEL_ID)
                .isPinned(false)
                .build();

        when(commentRepository.findById("comm_1")).thenReturn(Optional.of(comment));
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));

        assertThrows(AccessDeniedException.class, () ->
                reelService.pinComment("comm_1", USER_ID, false)
        );

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reels Silme: Satıcı kendi videosunu silebilmeli, yorumlar ve cache temizlenmeli")
    void deleteReel_WhenSeller_ShouldDeleteReelAndComments() {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));

        reelService.deleteReel(REEL_ID, SELLER_ID, false);

        verify(commentRepository, times(1)).deleteAllByReelId(REEL_ID);
        verify(reelRepository, times(1)).delete(reel);
        verify(cacheService, times(1)).deleteByPattern("cache:reels:feed:*");
    }

    @Test
    @DisplayName("Reels Feed Cache HIT: Redis'te feed varsa DB ve Feign çağrılmamalı")
    void getReelsFeed_WhenCacheHit_ShouldReturnCachedData() {
        Pageable pageable = PageRequest.of(0, 20);
        String cacheKey = "cache:reels:feed:page_0_size_20";

        CachedReelFeedResponse cachedFeed = CachedReelFeedResponse.builder()
                .content(List.of(reelResponse))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .last(true)
                .build();

        when(cacheService.get(cacheKey)).thenReturn(cachedFeed);
        when(objectMapper.convertValue(eq(cachedFeed), eq(CachedReelFeedResponse.class))).thenReturn(cachedFeed);

        Page<ReelResponse> result = reelService.getReelsFeed(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(REEL_ID);

        verify(cacheService, times(1)).get(cacheKey);
        verify(reelRepository, never()).findAllByStatus(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Reels Feed Cache MISS: Redis'te feed yoksa DB'den çekilmeli ve cache'e yazılmalı")
    void getReelsFeed_WhenCacheMiss_ShouldFetchFromDbAndSetCache() {
        Pageable pageable = PageRequest.of(0, 20);
        String cacheKey = "cache:reels:feed:page_0_size_20";

        when(cacheService.get(cacheKey)).thenReturn(null);
        when(reelRepository.findAllByStatus(eq("ACTIVE"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(reel), pageable, 1));
        when(commentRepository.countByReelId(REEL_ID)).thenReturn(0L);

        Page<ReelResponse> result = reelService.getReelsFeed(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(cacheService, times(1)).get(cacheKey);
        verify(reelRepository, times(1)).findAllByStatus(eq("ACTIVE"), eq(pageable));
        verify(cacheService, times(1)).set(eq(cacheKey), any(CachedReelFeedResponse.class), eq(1800L));
    }
}