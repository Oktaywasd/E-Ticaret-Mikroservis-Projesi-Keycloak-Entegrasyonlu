package com.ecommerce.media.e2e;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.request.CreateCommentRequest;
import com.ecommerce.media.dto.request.CreateReelRequest;
import com.ecommerce.media.dto.response.ReelCommentResponse;
import com.ecommerce.media.dto.response.ReelResponse;
import com.ecommerce.media.model.Reel;
import com.ecommerce.media.model.ReelComment;
import com.ecommerce.media.repository.ReelCommentRepository;
import com.ecommerce.media.repository.ReelRepository;
import com.ecommerce.media.service.FileStorageService;
import com.ecommerce.media.service.impl.ReelServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = MediaSocialWorkflowE2ETest.TestConfig.class)
@TestPropertySource(properties = {
        "application.clients.order.url=http://localhost:9572",
        "application.clients.product-catalog.url=http://localhost:9572"
})
class MediaSocialWorkflowE2ETest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Configuration
    @EnableFeignClients(clients = {OrderClient.class, ProductCatalogClient.class})
    @Import({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {
    }

    // Java 23 Mockito Byte Buddy kısıtını aşmak için Fake Storage Service
    static class FakeFileStorageService extends FileStorageService {
        public FakeFileStorageService() {
            super(null, null);
        }

        @Override
        public String uploadVideo(MultipartFile file) {
            return "http://minio/videos/sample.mp4";
        }

        @Override
        public String uploadThumbnail(MultipartFile file) {
            return "http://minio/thumbnails/sample.jpg";
        }
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private ProductCatalogClient productCatalogClient;

    @Mock
    private ReelRepository reelRepository;

    @Mock
    private ReelCommentRepository commentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private ReelServiceImpl reelService;
    private FileStorageService fileStorageService;

    private final String PRODUCT_ID = "prod_media_888";
    private final String SELLER_USER_ID = "seller_media_001";
    private final String BUYER_USER_ID = "user_buyer_777";
    private final String REEL_ID = "reel_video_123";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9572));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9572);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wireMockServer.resetAll();

        fileStorageService = new FakeFileStorageService();

        reelService = new ReelServiceImpl(
                reelRepository,
                commentRepository,
                fileStorageService,
                productCatalogClient,
                orderClient,
                mongoTemplate
        );
    }

    @Test
    @DisplayName("Senaryo 1 (Admin/Satıcı Video Yükleme & Ürün Bağlama): Video MinIO'ya yüklenir, ürün bilgisi çekilir ve kaydedilir")
    void uploadReelWorkflow_ShouldUploadToMinioAndSaveMetadata() {
        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + PRODUCT_ID + "\",\"name\":\"Trend Sneaker\",\"thumbnailUrl\":\"http://minio/thumbs/default.jpg\"}")));

        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> {
            Reel r = invocation.getArgument(0);
            r.setId(REEL_ID);
            return r;
        });

        CreateReelRequest request = new CreateReelRequest();
        request.setTitle("Yeni Sezon Sneaker İncelemesi");
        request.setDescription("Harika bir ayakkabı!");
        request.setProductId(PRODUCT_ID);
        request.setDurationInSeconds(30);

        MockMultipartFile videoFile = new MockMultipartFile("video", "video.mp4", "video/mp4", "fake-video-content".getBytes());
        MockMultipartFile thumbnailFile = new MockMultipartFile("thumbnail", "thumb.jpg", "image/jpeg", "fake-thumb-content".getBytes());

        ReelResponse response = reelService.uploadReel(request, videoFile, thumbnailFile, SELLER_USER_ID);

        assertNotNull(response);
        assertEquals(REEL_ID, response.getId());
        assertEquals("http://minio/videos/sample.mp4", response.getVideoUrl());
        assertEquals(PRODUCT_ID, response.getProductId());
        assertEquals("Trend Sneaker", response.getProductName());

        verify(reelRepository, times(1)).save(any(Reel.class));
    }

    @Test
    @DisplayName("Senaryo 1 (Sosyal Etkileşim): Satın almış müşteri videoya doğrulanmış yorum bırakır ve beğeni atar")
    void commentAndLikeWorkflow_ShouldAddVerifiedCommentAndToggleLikes() {
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + BUYER_USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Reel mockReel = Reel.builder()
                .id(REEL_ID)
                .productId(PRODUCT_ID)
                .likeCount(10L)
                .viewCount(100L)
                .likedUserIds(new HashSet<>())
                .viewedUserIds(new HashSet<>())
                .build();

        when(reelRepository.findById(REEL_ID)).thenReturn(Optional.of(mockReel));
        when(reelRepository.save(any(Reel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(commentRepository.save(any(ReelComment.class))).thenAnswer(invocation -> {
            ReelComment c = invocation.getArgument(0);
            c.setId("comm_999");
            return c;
        });

        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("Kalıbı tam, tavsiye ederim!");
        ReelCommentResponse commentResp = reelService.addComment(REEL_ID, commentReq, BUYER_USER_ID, "Ali");

        assertNotNull(commentResp);
        assertEquals("comm_999", commentResp.getId());
        assertTrue(commentResp.getIsVerifiedBuyer());

        reelService.toggleLikeReel(REEL_ID, BUYER_USER_ID);
        assertEquals(11L, mockReel.getLikeCount());
        assertTrue(mockReel.getLikedUserIds().contains(BUYER_USER_ID));
    }
}
