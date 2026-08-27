package com.ecommerce.media.service;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.exception.ResourceNotFoundException;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.impl.ReelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReelServiceTest {

    @Mock
    private ReelRepository reelRepository;

    @Mock
    private ReelCommentRepository commentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ReelServiceImpl reelService;

    private Reel reel;
    private final String REEL_ID = "reel_101";
    private final String USER_ID = "user_123";
    private final String SELLER_ID = "seller_999";
    private final String PRODUCT_ID = "prod_001";

    @BeforeEach
    void setUp() {
        reel = Reel.builder()
                .id(REEL_ID)
                .title("Harika Ürün Tanıtımı")
                .description("Bu tişörtü mutlaka deneyin!")
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .likeCount(0L)
                .viewCount(0L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("İzlenme Artışı: incrementViewCount çağrıldığında viewCount artmalı ve kullanıcı listeye eklenmeli")
    void incrementViewCount_WhenCalled_ShouldIncreaseViewCountAndAddUser() {
        // Arrange
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        // Act
        reelService.incrementViewCount(REEL_ID, USER_ID);

        // Assert
        assertThat(reel.getViewCount()).isEqualTo(1L);
        assertThat(reel.getViewedUserIds()).contains(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Beğeni Ekleme: İlk kez beğenen kullanıcının beğenisi likeCount'ı artırmalı")
    void toggleLikeReel_WhenNotLikedBefore_ShouldAddLike() {
        // Arrange
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        // Act
        reelService.toggleLikeReel(REEL_ID, USER_ID);

        // Assert
        assertThat(reel.getLikeCount()).isEqualTo(1L);
        assertThat(reel.getLikedUserIds()).contains(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Beğeni Geri Çekme: Zaten beğenmiş kullanıcı tekrar basarsa beğeni geri alınmalı")
    void toggleLikeReel_WhenAlreadyLiked_ShouldRemoveLike() {
        // Arrange
        reel.setLikeCount(1L);
        reel.getLikedUserIds().add(USER_ID);
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(reelRepository.save(any(Reel.class))).thenReturn(reel);

        // Act
        reelService.toggleLikeReel(REEL_ID, USER_ID);

        // Assert
        assertThat(reel.getLikeCount()).isEqualTo(0L);
        assertThat(reel.getLikedUserIds()).doesNotContain(USER_ID);
        verify(reelRepository, times(1)).save(reel);
    }

    @Test
    @DisplayName("Reels Mavi Tik: Ürünü satın almış kullanıcı yorum yaptığında isVerifiedBuyer true olmalı")
    void addComment_WhenUserPurchasedProduct_ShouldSetVerifiedBuyerTrue() {
        // Arrange
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

        // Act
        ReelCommentResponse response = reelService.addComment(REEL_ID, request, USER_ID, "ahmet");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIsVerifiedBuyer()).isTrue();
        verify(orderClient, times(1)).verifyPurchase(USER_ID, PRODUCT_ID);
        verify(commentRepository, times(1)).save(any(ReelComment.class));
    }

    @Test
    @DisplayName("Yorum Sabitleme Yetkisi: Satıcı kendi videosundaki yorumu sabitleyebilmeli")
    void pinComment_WhenSeller_ShouldTogglePin() {
        // Arrange
        ReelComment comment = ReelComment.builder()
                .id("comm_1")
                .reelId(REEL_ID)
                .isPinned(false)
                .build();

        when(commentRepository.findById("comm_1")).thenReturn(Optional.of(comment));
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));
        when(commentRepository.save(any(ReelComment.class))).thenReturn(comment);

        // Act
        reelService.pinComment("comm_1", SELLER_ID, false);

        // Assert
        assertThat(comment.getIsPinned()).isTrue();
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    @DisplayName("Yorum Sabitleme Yetkisiz Erişim: Başka bir kullanıcı yorum sabitlemeye çalışırsa AccessDeniedException fırlatmalı")
    void pinComment_WhenNotSellerAndNotAdmin_ShouldThrowAccessDeniedException() {
        // Arrange
        ReelComment comment = ReelComment.builder()
                .id("comm_1")
                .reelId(REEL_ID)
                .isPinned(false)
                .build();

        when(commentRepository.findById("comm_1")).thenReturn(Optional.of(comment));
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));

        // Act & Assert (USER_ID satıcı değil ve admin değil)
        assertThrows(AccessDeniedException.class, () -> {
            reelService.pinComment("comm_1", USER_ID, false);
        });

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reels Silme: Satıcı kendi videosunu silebilmeli ve videoya ait yorumlar da silinmeli")
    void deleteReel_WhenSeller_ShouldDeleteReelAndComments() {
        // Arrange
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(reel));

        // Act
        reelService.deleteReel(REEL_ID, SELLER_ID, false);

        // Assert
        verify(commentRepository, times(1)).deleteAllByReelId(REEL_ID);
        verify(reelRepository, times(1)).delete(reel);
    }
}