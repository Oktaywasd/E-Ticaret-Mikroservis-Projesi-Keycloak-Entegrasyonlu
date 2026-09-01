package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.mapper.ProductMapper;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Stock;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.service.CacheService;
import com.ecommerce.productcatalog.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductPopularityCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private CacheService cacheService;

    @MockBean
    private FileStorageService fileStorageService;

    @SpyBean
    private ProductMapper productMapper;

    private Product sampleProduct1;
    private Product sampleProduct2;
    private static final String CACHE_TOP_PRODUCTS = "cache:top_products";
    private static final String CACHE_TOP_50_PRODUCTS = "cache:top_50_products";

    @BeforeEach
    void setUp() {
        Stock stock1 = new Stock();
        stock1.setCurrentStock(20);

        Stock stock2 = new Stock();
        stock2.setCurrentStock(50);

        sampleProduct1 = Product.builder()
                .id("prod_top_1")
                .name("En Çok Satan Akıllı Telefon")
                .brand("TechBrand")
                .productCode("TECH-001")
                .categoryId("cat_electronics")
                .isActive(true)
                .isDeleted(false)
                .salesCount(150)
                .popularityScore(1800.0)
                .stock(stock1)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        sampleProduct2 = Product.builder()
                .id("prod_top_2")
                .name("Popüler Bluetooth Kulaklık")
                .brand("SoundBrand")
                .productCode("SND-002")
                .categoryId("cat_electronics")
                .isActive(true)
                .isDeleted(false)
                .salesCount(95)
                .popularityScore(1100.0)
                .stock(stock2)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. Top 10 Cache Miss: Redis'te veri yokken DB'den çekilmeli ve cache:top_products'a yazılmalı")
    void getTop10Products_WhenCacheMiss_ShouldFetchFromDbAndSetToCache() throws Exception {
        when(cacheService.get(CACHE_TOP_PRODUCTS)).thenReturn(null);

        when(productRepository.findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc())
                .thenReturn(List.of(sampleProduct1, sampleProduct2));

        mockMvc.perform(get("/api/v1/products/top")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prod_top_1"))
                .andExpect(jsonPath("$[0].name").value("En Çok Satan Akıllı Telefon"))
                .andExpect(jsonPath("$[0].popularityScore").value(1800.0))
                .andExpect(jsonPath("$[1].id").value("prod_top_2"));

        verify(productRepository, times(1)).findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        verify(cacheService, times(1)).set(eq(CACHE_TOP_PRODUCTS), any(), anyLong());
    }

    @Test
    @DisplayName("2. Top 10 Cache Hit: Redis'te veri varken DB sorgusu tetiklenmeden doğrudan cache'ten dönmeli")
    void getTop10Products_WhenCacheHit_ShouldReturnFromCacheWithoutDbCall() throws Exception {
        ProductResponse cachedResponse = ProductResponse.builder()
                .id("cached_prod_99")
                .name("Önbellekteki Efsane Ürün")
                .popularityScore(2500.0)
                .build();

        when(cacheService.get(CACHE_TOP_PRODUCTS)).thenReturn(List.of(cachedResponse));

        mockMvc.perform(get("/api/v1/products/top")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cached_prod_99"))
                .andExpect(jsonPath("$[0].name").value("Önbellekteki Efsane Ürün"))
                .andExpect(jsonPath("$[0].popularityScore").value(2500.0));

        verify(productRepository, never()).findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        verify(cacheService, never()).set(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("3. Top 50 Cache Akışı: /top-50 çağrıldığında cache:top_50_products anahtarı ile çalışmalı")
    void getTop50Products_WhenCacheMiss_ShouldSetToTop50Cache() throws Exception {
        when(cacheService.get(CACHE_TOP_50_PRODUCTS)).thenReturn(null);
        when(productRepository.findTop50ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc())
                .thenReturn(List.of(sampleProduct1));

        mockMvc.perform(get("/api/v1/products/top-50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prod_top_1"));

        verify(cacheService, times(1)).set(eq(CACHE_TOP_50_PRODUCTS), any(), anyLong());
    }

    @Test
    @DisplayName("4. Satış ve Stok Düşme: Stok düşürüldüğünde hem Top 10 hem Top 50 cache temizlenmeli")
    void reduceStock_ShouldUpdateScoreAndInvalidateBothCaches() throws Exception {
        when(productRepository.findById("prod_top_1")).thenReturn(Optional.of(sampleProduct1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/v1/products/{id}/reduce-stock", "prod_top_1")
                        .param("quantity", "2")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(b -> b.subject("order_service_client"))))
                .andExpect(status().isOk());

        verify(productRepository, times(1)).save(argThat(p ->
                p.getStock().getCurrentStock() == 18 && p.getSalesCount() == 152
        ));
        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }

    @Test
    @DisplayName("5. Admin Cache Temizleme: /admin/cache/clear endpoint'i tüm popülerlik cache'lerini silmeli")
    void clearCache_WhenAdmin_ShouldDeleteAllProductCaches() throws Exception {
        mockMvc.perform(delete("/api/v1/products/admin/cache/clear")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(b -> b.subject("admin_user"))))
                .andExpect(status().isNoContent());

        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }
}