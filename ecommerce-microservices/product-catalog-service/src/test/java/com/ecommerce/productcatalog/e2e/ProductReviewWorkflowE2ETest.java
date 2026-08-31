package com.ecommerce.productcatalog.e2e;

import com.ecommerce.productcatalog.client.OrderClient;
import com.ecommerce.productcatalog.dto.request.ReviewCreateRequest;
import com.ecommerce.productcatalog.dto.response.ReviewResponse;
import com.ecommerce.productcatalog.exception.BusinessException;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Review;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.repository.ReviewRepository;
import com.ecommerce.productcatalog.service.CacheService;
import com.ecommerce.productcatalog.service.impl.ReviewServiceImpl;
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
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ProductReviewWorkflowE2ETest.TestConfig.class)
@TestPropertySource(properties = {
        "order-service.url=http://localhost:9571"
})
class ProductReviewWorkflowE2ETest {

    @Configuration
    @EnableFeignClients(clients = OrderClient.class)
    @Import({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private OrderClient orderClient;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewServiceImpl reviewService;

    private final String PRODUCT_ID = "prod_review_100";
    private final String BUYER_USER_ID = "user_buyer_200";
    private final String NON_BUYER_USER_ID = "user_fake_300";

    private Product testProduct;

    private CacheService cacheService;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9571));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9571);
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

        reviewService = new ReviewServiceImpl(reviewRepository, productRepository, orderClient, cacheService);

        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Akıllı Saat Pro")
                .isDeleted(false)
                .ratingAverage(4.0)
                .reviewCount(1)
                .build();
    }

    @Test
    @DisplayName("Senaryo 1 (Happy Path): Satın almış kullanıcı 5 yıldız verir, mavi tik alır ve ürün puanı güncellenir")
    void createReview_WhenUserPurchasedProduct_ShouldCreateVerifiedReviewAndUpdateRating() {
        // WireMock: Order Service "true" dönüyor (satın almış)
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + BUYER_USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId("rev_e2e_555");
            return r;
        });

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setComment("Mükemmel batarya ömrü, kesinlikle tavsiye ederim.");

        ReviewResponse response = reviewService.createReview(PRODUCT_ID, request, BUYER_USER_ID, "Mehmet");

        assertNotNull(response);
        assertEquals("rev_e2e_555", response.getId());
        assertTrue(response.isVerifiedBuyer());
        assertEquals(5, response.getRating());

        // Puan Güncelleme Doğrulaması: ((4.0 * 1) + 5) / 2 = 4.5
        assertEquals(4.5, testProduct.getRatingAverage());
        assertEquals(2, testProduct.getReviewCount());

        verify(productRepository, times(1)).save(testProduct);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Senaryo 4 (Güvenlik Kalkanı): Siparişi olmayan kullanıcı yorum yapmaya kalkarsa BusinessException ile engellenir")
    void createReview_WhenUserDidNotPurchaseProduct_ShouldThrowBusinessException() {
        // WireMock: Order Service "false" dönüyor (satın almamış)
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + NON_BUYER_USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));

        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(1);
        request.setComment("Ürünü beğenmedim.");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                reviewService.createReview(PRODUCT_ID, request, NON_BUYER_USER_ID, "SahteHesap")
        );

        assertTrue(exception.getMessage().contains("Yalnızca ürünü sipariş etmiş olan kullanıcılar"));
        verify(reviewRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }
}