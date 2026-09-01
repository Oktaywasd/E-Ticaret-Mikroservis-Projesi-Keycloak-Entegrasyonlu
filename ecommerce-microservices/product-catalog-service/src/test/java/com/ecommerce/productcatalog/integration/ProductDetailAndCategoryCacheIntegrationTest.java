package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.dto.PriceDto;
import com.ecommerce.productcatalog.dto.StockDto;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.mapper.ProductMapper;
import com.ecommerce.productcatalog.model.Price;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Stock;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.ecommerce.productcatalog.repository.ReviewRepository;
import com.ecommerce.productcatalog.service.CacheService;
import com.ecommerce.productcatalog.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductDetailAndCategoryCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @SpyBean
    private ProductMapper productMapper;

    private final String PROD_ID = "prod_detail_001";
    private final String CAT_ID = "cat_tech_999";
    private static final String CACHE_TOP_PRODUCTS = "cache:top_products";
    private static final String CACHE_TOP_50_PRODUCTS = "cache:top_50_products";

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        Stock stock = new Stock();
        stock.setCurrentStock(10);

        Price price = new Price();
        price.setSellingPrice(BigDecimal.valueOf(750.0));
        price.setDiscountedPrice(BigDecimal.valueOf(699.0));

        sampleProduct = Product.builder()
                .id(PROD_ID)
                .name("Kablosuz Mekanik Klavye")
                .brand("KeyTech")
                .productCode("KB-101")
                .categoryId(CAT_ID)
                .isActive(true)
                .isDeleted(false)
                .salesCount(20)
                .popularityScore(250.0)
                .price(price)
                .stock(stock)
                .imageUrls(new ArrayList<>(List.of("https://minio.test/kb1.jpg")))
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. ID ile Ürün Detayı Getirme: Ürün detayı başarıyla getirilmeli")
    void getProductById_WhenExists_ShouldReturnProduct() throws Exception {
        when(productRepository.findByIdAndIsDeletedFalse(PROD_ID)).thenReturn(Optional.of(sampleProduct));

        mockMvc.perform(get("/api/v1/products/{id}", PROD_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROD_ID))
                .andExpect(jsonPath("$.name").value("Kablosuz Mekanik Klavye"))
                .andExpect(jsonPath("$.brand").value("KeyTech"));

        verify(productRepository, times(1)).findByIdAndIsDeletedFalse(PROD_ID);
    }

    @Test
    @DisplayName("2. Ürün Güncelleme (Eviction): Ürün güncellendiğinde tüm ilgili Redis cache'leri silinmeli")
    void updateProduct_WhenValid_ShouldInvalidateProductCaches() throws Exception {
        when(productRepository.findByIdAndIsDeletedFalse(PROD_ID)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        PriceDto priceDto = PriceDto.builder()
                .sellingPrice(BigDecimal.valueOf(750.0))
                .discountedPrice(BigDecimal.valueOf(699.0))
                .build();

        StockDto stockDto = StockDto.builder()
                .currentStock(10)
                .minimumStock(2)
                .build();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("Kablosuz Mekanik Klavye V2 RGB")
                .categoryId(CAT_ID)
                .brand("KeyTech Pro")
                .price(priceDto)
                .stock(stockDto)
                .build();

        mockMvc.perform(patch("/api/v1/products/{id}", PROD_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SELLER"))
                                .jwt(b -> b.subject("seller_123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROD_ID));

        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }

    @Test
    @DisplayName("3. Ürün Silme (Eviction): Ürün soft-delete yapıldığında cache'ler anında temizlenmeli")
    void deleteProduct_WhenAuthorized_ShouldInvalidateCaches() throws Exception {
        when(productRepository.findByIdAndIsDeletedFalse(PROD_ID)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(delete("/api/v1/products/{id}", PROD_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(b -> b.subject("admin_user"))))
                .andExpect(status().isNoContent());

        verify(productRepository, times(1)).save(argThat(Product::getIsDeleted));
        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }

    @Test
    @DisplayName("4. Görsel Yükleme (Eviction): Ürüne görsel eklendiğinde cache temizlenmeli")
    void uploadProductImages_ShouldInvalidateCaches() throws Exception {
        when(productRepository.findByIdAndIsDeletedFalse(PROD_ID)).thenReturn(Optional.of(sampleProduct));
        when(fileStorageService.uploadImage(any())).thenReturn("https://minio.test/kb2.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile imageFile = new MockMultipartFile(
                "files", "klavye.jpg", "image/jpeg", "image byte data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/products/{id}/images", PROD_ID)
                        .file(imageFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                                .jwt(b -> b.subject("seller_123"))))
                .andExpect(status().isOk());

        verify(cacheService, times(1)).delete(CACHE_TOP_PRODUCTS);
        verify(cacheService, times(1)).delete(CACHE_TOP_50_PRODUCTS);
    }
}