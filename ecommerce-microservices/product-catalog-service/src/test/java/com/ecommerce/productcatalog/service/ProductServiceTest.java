package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.exception.AlreadyExistsException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.mapper.ProductMapper;
import com.ecommerce.productcatalog.model.Price;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Stock;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductCreateRequest createRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "topProductsTtl", 3600L);
        ReflectionTestUtils.setField(productService, "top50ProductsTtl", 21600L);

        Price price = new Price();
        price.setSellingPrice(BigDecimal.valueOf(250.00));

        Stock stock = new Stock();
        stock.setCurrentStock(20);

        product = Product.builder()
                .id("prod_101")
                .productCode("PRD-001")
                .name("Oversize Tişört")
                .categoryId("cat_123")
                .price(price)
                .stock(stock)
                .salesCount(10)
                .popularityScore(100.0)
                .isDeleted(false)
                .isActive(true)
                .build();

        createRequest = new ProductCreateRequest();
        createRequest.setProductCode("PRD-001");
        createRequest.setCategoryId("cat_123");
        createRequest.setName("Oversize Tişört");

        productResponse = ProductResponse.builder()
                .id("prod_101")
                .productCode("PRD-001")
                .name("Oversize Tişört")
                .salesCount(10)
                .popularityScore(100.0)
                .build();
    }

    @Test
    @DisplayName("Başarılı Ürün Oluşturma: Benzersiz kod ve geçerli kategori ile ürün kaydedilmeli")
    void createProduct_WhenValidRequest_ShouldReturnProductResponse() {
        // Arrange
        when(productRepository.existsByProductCodeAndIsDeletedFalse(createRequest.getProductCode())).thenReturn(false);
        when(categoryRepository.existsById(createRequest.getCategoryId())).thenReturn(true);
        when(productMapper.toEntity(createRequest)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act
        ProductResponse result = productService.createProduct(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductCode()).isEqualTo("PRD-001");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Mükerrer Ürün Kodu: Aynı productCode varsa AlreadyExistsException fırlatmalı")
    void createProduct_WhenDuplicateProductCode_ShouldThrowAlreadyExistsException() {
        // Arrange
        when(productRepository.existsByProductCodeAndIsDeletedFalse(createRequest.getProductCode())).thenReturn(true);

        // Act & Assert
        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class, () ->
                productService.createProduct(createRequest)
        );

        assertThat(exception.getMessage()).contains("aktif bir ürün zaten var");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Kategori Bulunamadı: Geçersiz kategori ID'si ile ürün oluşturulurken ResourceNotFoundException fırlatmalı")
    void createProduct_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(productRepository.existsByProductCodeAndIsDeletedFalse(createRequest.getProductCode())).thenReturn(false);
        when(categoryRepository.existsById(createRequest.getCategoryId())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.createProduct(createRequest)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Başarılı Stok Düşürme (reduceStock): Yeterli stok varsa miktar eksiltilmeli")
    void reduceStock_WhenSufficientStock_ShouldDecreaseStock() {
        // Arrange
        when(productRepository.findById("prod_101")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.reduceStock("prod_101", 5);

        // Assert
        assertThat(product.getStock().getCurrentStock()).isEqualTo(15);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Yetersiz Stok (reduceStock): Mevcut stoktan fazla adet istenirse AlreadyExistsException fırlatmalı")
    void reduceStock_WhenInsufficientStock_ShouldThrowAlreadyExistsException() {
        // Arrange
        when(productRepository.findById("prod_101")).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () ->
                productService.reduceStock("prod_101", 50)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Stok İade Etme (restoreStock): Sipariş iptalinde stok artırılmalı")
    void restoreStock_WhenValidProduct_ShouldIncreaseStock() {
        // Arrange
        when(productRepository.findById("prod_101")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.restoreStock("prod_101", 10);

        // Assert
        assertThat(product.getStock().getCurrentStock()).isEqualTo(30);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Soft Delete: Silme işleminde isDeleted true olmalı, fiziksel silme yapılmamalı")
    void deleteProduct_WhenValidId_ShouldSetIsDeletedTrue() {
        // Arrange
        when(productRepository.findByIdAndIsDeletedFalse("prod_101")).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct("prod_101");

        // Assert
        assertThat(product.getIsDeleted()).isTrue();
        verify(productRepository, times(1)).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("Durum Değiştirme (toggleProductStatus): Aktif ürün pasife alınabilmeli")
    void toggleProductStatus_WhenActive_ShouldBecomeInactive() {
        // Arrange
        when(productRepository.findByIdAndIsDeletedFalse("prod_101")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.toggleProductStatus("prod_101");

        // Assert
        assertThat(product.getIsActive()).isFalse();
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("ID ile Ürün Getirme: Olmayan ID arandığında ResourceNotFoundException fırlatmalı")
    void getProductById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(productRepository.findByIdAndIsDeletedFalse("unknown_id")).thenReturn(Optional.empty());
        when(productRepository.findById("unknown_id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductById("unknown_id")
        );
    }

    @Test
    @DisplayName("Top 10 Cache HIT: Redis'te veri varsa DB çağrılmamalı")
    void getTopProducts_WhenCacheHit_ShouldReturnCachedData() {
        // Arrange
        List<ProductResponse> cachedList = List.of(productResponse);
        when(cacheService.get("cache:top_products")).thenReturn(cachedList);
        when(objectMapper.convertValue(eq(cachedList), any(TypeReference.class))).thenReturn(cachedList);

        // Act
        List<ProductResponse> result = productService.getTopProducts(10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Oversize Tişört");

        verify(cacheService, times(1)).get("cache:top_products");
        verify(productRepository, never()).findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
    }

    @Test
    @DisplayName("Top 10 Cache MISS: Redis'te veri yoksa DB'den çekilip Redis'e kaydedilmeli")
    void getTopProducts_WhenCacheMiss_ShouldFetchFromDbAndSetCache() {
        // Arrange
        when(cacheService.get("cache:top_products")).thenReturn(null);
        when(productRepository.findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc())
                .thenReturn(List.of(product));
        when(productMapper.toResponseList(anyList())).thenReturn(List.of(productResponse));

        // Act
        List<ProductResponse> result = productService.getTopProducts(10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Oversize Tişört");

        verify(cacheService, times(1)).get("cache:top_products");
        verify(productRepository, times(1)).findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        verify(cacheService, times(1)).set(eq("cache:top_products"), anyList(), eq(3600L));
    }

    @Test
    @DisplayName("Top 50 Cache HIT: Redis'te veri varsa DB çağrılmamalı")
    void getTop50Products_WhenCacheHit_ShouldReturnCachedData() {
        // Arrange
        List<ProductResponse> cachedList = List.of(productResponse);
        when(cacheService.get("cache:top_50_products")).thenReturn(cachedList);
        when(objectMapper.convertValue(eq(cachedList), any(TypeReference.class))).thenReturn(cachedList);

        // Act
        List<ProductResponse> result = productService.getTop50Products();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Oversize Tişört");

        verify(cacheService, times(1)).get("cache:top_50_products");
        verify(productRepository, never()).findTop50ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
    }

    @Test
    @DisplayName("Top 50 Cache MISS: Redis'te veri yoksa DB'den çekilip Redis'e kaydedilmeli")
    void getTop50Products_WhenCacheMiss_ShouldFetchFromDbAndSetCache() {
        // Arrange
        when(cacheService.get("cache:top_50_products")).thenReturn(null);
        when(productRepository.findTop50ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc())
                .thenReturn(List.of(product));
        when(productMapper.toResponseList(anyList())).thenReturn(List.of(productResponse));

        // Act
        List<ProductResponse> result = productService.getTop50Products();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Oversize Tişört");

        verify(cacheService, times(1)).get("cache:top_50_products");
        verify(productRepository, times(1)).findTop50ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        verify(cacheService, times(1)).set(eq("cache:top_50_products"), anyList(), eq(21600L));
    }
}