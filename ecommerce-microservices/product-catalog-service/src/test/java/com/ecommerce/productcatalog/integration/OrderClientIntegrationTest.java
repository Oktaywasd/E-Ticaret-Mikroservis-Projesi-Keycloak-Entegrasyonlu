package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.client.OrderClient;
import com.ecommerce.productcatalog.dto.request.ReviewCreateRequest;
import com.ecommerce.productcatalog.dto.response.ReviewResponse;
import com.ecommerce.productcatalog.exception.BusinessException;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Review;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.repository.ReviewRepository;
import com.ecommerce.productcatalog.service.impl.ReviewServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = OrderClientIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "order-service.url=http://localhost:9561"
})
class OrderClientIntegrationTest {

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

    private final String PRODUCT_ID = "prod_999";
    private final String USER_ID = "user_888";
    private Product testProduct;
    private ReviewCreateRequest reviewRequest;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9561));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9561);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        reviewService = new ReviewServiceImpl(reviewRepository, productRepository, orderClient);

        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Sneaker")
                .isDeleted(false)
                .ratingAverage(4.0)
                .reviewCount(1)
                .build();

        reviewRequest = new ReviewCreateRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment("Mükemmel kalite!");
    }

    @Test
    @DisplayName("Sözleşme Başarılı: Order servisi true döndüğünde mavi tikli yorum oluşmalı ve puan güncellenmeli")
    void createReview_WhenOrderServiceReturnsTrue_ShouldSaveReviewAndUpdateRating() {
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId("rev_123");
            return r;
        });

        ReviewResponse response = reviewService.createReview(PRODUCT_ID, reviewRequest, USER_ID, "Ahmet");

        assertNotNull(response);
        assertEquals("rev_123", response.getId());
        assertTrue(response.isVerifiedBuyer());
        assertEquals(5, response.getRating());

        assertEquals(4.5, testProduct.getRatingAverage());
        assertEquals(2, testProduct.getReviewCount());

        verify(productRepository, times(1)).save(testProduct);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Sözleşme Red: Order servisi false döndüğünde BusinessException fırlatılmalı")
    void createReview_WhenOrderServiceReturnsFalse_ShouldThrowBusinessException() {
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));

        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                reviewService.createReview(PRODUCT_ID, reviewRequest, USER_ID, "Ahmet")
        );

        assertTrue(exception.getMessage().contains("Yalnızca ürünü sipariş etmiş olan kullanıcılar"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sözleşme Hata Yönetimi: Order servisi 500 döndüğünde BusinessException fırlatılmalı")
    void createReview_WhenOrderServiceFails_ShouldThrowBusinessException() {
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse().withStatus(500)));

        when(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                reviewService.createReview(PRODUCT_ID, reviewRequest, USER_ID, "Ahmet")
        );

        assertTrue(exception.getMessage().contains("Sipariş geçmişi doğrulanırken bir hata oluştu"));
        verify(reviewRepository, never()).save(any());
    }
}