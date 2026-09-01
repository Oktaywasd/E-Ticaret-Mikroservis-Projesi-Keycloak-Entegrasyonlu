package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.model.Price;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Stock;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.repository.ReviewRepository;
import com.ecommerce.productcatalog.service.CacheService;
import com.ecommerce.productcatalog.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductStockLockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private ReviewRepository reviewRepository;

    @MockBean
    private CacheService cacheService;

    @MockBean
    private FileStorageService fileStorageService;

    private final String PROD_ID = "prod_limited_stock_01";
    private Product sampleProduct;
    private static final String CACHE_TOP_PRODUCTS = "cache:top_products";
    private static final String CACHE_TOP_50_PRODUCTS = "cache:top_50_products";

    @BeforeEach
    void setUp() {
        Stock stock = new Stock();
        stock.setCurrentStock(1);
        stock.setMinimumStock(0);

        Price price = new Price();
        price.setSellingPrice(BigDecimal.valueOf(1500.0));
        price.setDiscountedPrice(BigDecimal.valueOf(1400.0));

        sampleProduct = Product.builder()
                .id(PROD_ID)
                .name("Sınırlı Sayıda Akıllı Saat")
                .brand("SmartWatch Co")
                .productCode("SW-LIMITED-01")
                .categoryId("cat_watch")
                .isActive(true)
                .isDeleted(false)
                .salesCount(10)
                .popularityScore(100.0)
                .price(price)
                .stock(stock)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. Başarılı Stok Düşümü: Yeterli stok varken stok düşmeli ve cache temizlenmeli")
    void reduceStock_WhenStockAvailable_ShouldReduceAndEvictCache() throws Exception {
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/v1/products/{id}/reduce-stock", PROD_ID)
                        .param("quantity", "1")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_SELLER"),
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                                .jwt(b -> b.subject("order_service")
                                        .claim("realm_access", Map.of("roles", List.of("ADMIN", "SELLER", "USER"))))))
                .andExpect(status().isOk());

        verify(productRepository, times(1)).save(argThat(p ->
                p.getStock().getCurrentStock() == 0 && p.getSalesCount() == 11
        ));
        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }

    @Test
    @DisplayName("2. Yetersiz Stok Durumu: Talep edilen miktar mevcut stoktan fazlaysa hata dönmeli")
    void reduceStock_WhenInsufficientStock_ShouldReturnBadRequest() throws Exception {
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(sampleProduct));

        mockMvc.perform(put("/api/v1/products/{id}/reduce-stock", PROD_ID)
                        .param("quantity", "2")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_SELLER"),
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                                .jwt(b -> b.subject("order_service")
                                        .claim("realm_access", Map.of("roles", List.of("ADMIN", "SELLER", "USER"))))))
                .andExpect(status().is4xxClientError());

        verify(productRepository, never()).save(any(Product.class));
        verify(cacheService, never()).delete(anyString());
    }

    @Test
    @DisplayName("3. Eşzamanlı Stok Yarışı Simülasyonu: Paralel isteklerde tüm çağrılar işlenmeli")
    void reduceStock_ConcurrentRequests_ShouldPreventOverSelling() throws Exception {
        when(productRepository.findById(PROD_ID)).thenAnswer(inv -> Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        int numberOfThreads = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(put("/api/v1/products/{id}/reduce-stock", PROD_ID)
                                    .param("quantity", "1")
                                    .with(jwt()
                                            .authorities(
                                                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                                                    new SimpleGrantedAuthority("ROLE_SELLER"),
                                                    new SimpleGrantedAuthority("ROLE_USER")
                                            )
                                            .jwt(b -> b.subject("order_service")
                                                    .claim("realm_access", Map.of("roles", List.of("ADMIN", "SELLER", "USER"))))))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(4, successCount.get() + failureCount.get());
    }

    @Test
    @DisplayName("4. Stok İade (Restore Stock): İptal durumunda stok artmalı ve cache temizlenmeli")
    void restoreStock_WhenValid_ShouldIncreaseStockAndEvictCache() throws Exception {
        sampleProduct.getStock().setCurrentStock(0);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/v1/products/{id}/restore-stock", PROD_ID)
                        .param("quantity", "2")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_SELLER"),
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                                .jwt(b -> b.subject("order_service")
                                        .claim("realm_access", Map.of("roles", List.of("ADMIN", "SELLER", "USER"))))))
                .andExpect(status().isOk());

        verify(productRepository, times(1)).save(argThat(p ->
                p.getStock().getCurrentStock() == 2
        ));
        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }
}