package com.ecommerce.media.integration;

import com.ecommerce.media.client.OrderClient;
import com.ecommerce.media.client.ProductCatalogClient;
import com.ecommerce.media.dto.response.ProductSummaryResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MediaClientsIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "application.clients.order.url=http://localhost:9563",
        "application.clients.product-catalog.url=http://localhost:9563"
})
class MediaClientsIntegrationTest {

    @Configuration
    @EnableFeignClients(clients = {OrderClient.class, ProductCatalogClient.class})
    @Import({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private ProductCatalogClient productCatalogClient;

    private final String USER_ID = "user_media_101";
    private final String PRODUCT_ID = "prod_media_202";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9563));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9563);
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
    }

    @Test
    @DisplayName("Sözleşme Başarılı: OrderClient doğrulanmış alıcı durumunu doğru çözümlemeli")
    void verifyPurchase_WhenPurchased_ShouldReturnTrue() {
        stubFor(get(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + USER_ID + "&productId=" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean isBuyer = orderClient.verifyPurchase(USER_ID, PRODUCT_ID);

        assertNotNull(isBuyer);
        assertTrue(isBuyer);
        verify(getRequestedFor(urlEqualTo("/api/v1/orders/internal/verify-purchase?userId=" + USER_ID + "&productId=" + PRODUCT_ID)));
    }

    @Test
    @DisplayName("Sözleşme Başarılı: ProductCatalogClient ürün özet bilgilerini JSON'dan çözümlemeli")
    void getProductSummaryById_WhenProductExists_ShouldReturnSummaryDto() {
        String productJsonResponse = """
                {
                    "id": "%s",
                    "name": "Oversize Vintage T-Shirt",
                    "price": {
                        "sellingPrice": 450.00,
                        "discountedPrice": 399.90
                    },
                    "mainImageUrl": "http://minio/products/tshirt.png"
                }
                """.formatted(PRODUCT_ID);

        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJsonResponse)));

        ProductSummaryResponse summary = productCatalogClient.getProductSummaryById(PRODUCT_ID);

        assertNotNull(summary);
        assertEquals(PRODUCT_ID, summary.getId());
        verify(getRequestedFor(urlEqualTo("/api/v1/products/" + PRODUCT_ID)));
    }
}