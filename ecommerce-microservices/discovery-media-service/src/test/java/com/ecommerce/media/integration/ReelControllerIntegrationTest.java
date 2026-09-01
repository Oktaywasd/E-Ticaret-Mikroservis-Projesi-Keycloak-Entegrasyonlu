package com.ecommerce.media.integration;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.CacheService;
import com.ecommerce.media.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private final String REEL_ID = "reel_101";
    private final String PRODUCT_ID = "prod_999";
    private final String VERIFIED_USER_ID = "user_verified_123";
    private final String NORMAL_USER_ID = "user_normal_456";

    private Reel sampleReel;

    @BeforeEach
    void setUp() {
        sampleReel = Reel.builder()
                .id(REEL_ID)
                .title("Harika Kumaş İncelemesi")
                .description("Ürün detay videosu")
                .productId(PRODUCT_ID)
                .sellerId("seller_001")
                .likeCount(5L)
                .viewCount(100L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Mavi Tik Entegrasyonu: Ürünü satın alan kullanıcı yorum yaptığında isVerifiedBuyer=true dönmeli")
    void addComment_WhenUserPurchasedProduct_ShouldReturnVerifiedBadge() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));
        when(reelCommentRepository.save(any(ReelComment.class))).thenAnswer(invocation -> {
            ReelComment c = invocation.getArgument(0);
            c.setId("comment_001");
            c.setCreatedAt(Instant.now());
            return c;
        });

        // Feign OrderClient: Kullanıcı ürünü satın almış -> true
        when(orderClient.verifyPurchase(eq(VERIFIED_USER_ID), eq(PRODUCT_ID))).thenReturn(true);

        CreateCommentRequest commentRequest = new CreateCommentRequest();
        commentRequest.setContent("Kumaşı gerçekten çok kaliteli, tavsiye ederim!");

        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", REEL_ID)
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                        new SimpleGrantedAuthority("USER"),
                                        new SimpleGrantedAuthority("CUSTOMER")
                                )
                                .jwt(builder -> builder
                                        .subject(VERIFIED_USER_ID)
                                        .claim("preferred_username", "ahmet_yildiz")
                                        .claim("name", "Ahmet Yıldız")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "CUSTOMER")))
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("comment_001"))
                .andExpect(jsonPath("$.username").value("Ahmet Yıldız"))
                .andExpect(jsonPath("$.content").value("Kumaşı gerçekten çok kaliteli, tavsiye ederim!"))
                .andExpect(jsonPath("$.isVerifiedBuyer").value(true));

        verify(orderClient).verifyPurchase(eq(VERIFIED_USER_ID), eq(PRODUCT_ID));
    }

    @Test
    @DisplayName("Normal Yorum: Ürünü satın almayan kullanıcı yorum yaptığında isVerifiedBuyer=false dönmeli")
    void addComment_WhenUserNotPurchasedProduct_ShouldReturnUnverified() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));
        when(reelCommentRepository.save(any(ReelComment.class))).thenAnswer(invocation -> {
            ReelComment c = invocation.getArgument(0);
            c.setId("comment_002");
            c.setCreatedAt(Instant.now());
            return c;
        });

        // Feign OrderClient: Kullanıcı ürünü satın almamış -> false
        when(orderClient.verifyPurchase(eq(NORMAL_USER_ID), eq(PRODUCT_ID))).thenReturn(false);

        CreateCommentRequest commentRequest = new CreateCommentRequest();
        commentRequest.setContent("Kalıbı nasıl acaba?");

        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", REEL_ID)
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                        new SimpleGrantedAuthority("USER"),
                                        new SimpleGrantedAuthority("CUSTOMER")
                                )
                                .jwt(builder -> builder
                                        .subject(NORMAL_USER_ID)
                                        .claim("preferred_username", "mehmet_kaya")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "CUSTOMER")))
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("comment_002"))
                .andExpect(jsonPath("$.isVerifiedBuyer").value(false));

        verify(orderClient).verifyPurchase(eq(NORMAL_USER_ID), eq(PRODUCT_ID));
    }

    @Test
    @DisplayName("Beğeni Akışı: Giriş yapmış kullanıcı videoyu beğenebilmeli (200 OK)")
    void likeReel_WhenAuthenticated_ShouldReturnOk() throws Exception {
        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(sampleReel));
        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/reels/{id}/like", REEL_ID)
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                        new SimpleGrantedAuthority("USER"),
                                        new SimpleGrantedAuthority("CUSTOMER")
                                )
                                .jwt(builder -> builder
                                        .subject(VERIFIED_USER_ID)
                                        .claim("realm_access", Map.of("roles", List.of("USER", "CUSTOMER")))
                                )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Güvenlik: Token olmadan yorum atmaya çalışıldığında 401 Unauthorized dönmeli")
    void addComment_WithoutToken_ShouldReturn401() throws Exception {
        CreateCommentRequest commentRequest = new CreateCommentRequest();
        commentRequest.setContent("Yetkisiz yorum denemesi");

        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", REEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isUnauthorized());
    }
}