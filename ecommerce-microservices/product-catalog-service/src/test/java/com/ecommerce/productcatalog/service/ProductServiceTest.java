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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductCreateRequest createRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
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
                .build();
    }

    @Test
    @DisplayName("Başarılı Ürün Oluşturma: Benzersiz kod ve geçerli kategori ile ürün kaydedilmeli")
    void createProduct_WhenValidRequest_ShouldReturnProductResponse() {
        // Arrange (Given)
        when(productRepository.existsByProductCodeAndIsDeletedFalse(createRequest.getProductCode())).thenReturn(false);
        when(categoryRepository.existsById(createRequest.getCategoryId())).thenReturn(true);
        when(productMapper.toEntity(createRequest)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        // Act (When)
        ProductResponse result = productService.createProduct(createRequest);

        // Assert (Then)
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
        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class, () -> {
            productService.createProduct(createRequest);
        });

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
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProduct(createRequest);
        });

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
        assertThrows(AlreadyExistsException.class, () -> {
            productService.reduceStock("prod_101", 50); // Mevcut: 20, İstenen: 50
        });

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
        assertThat(product.getStock().getCurrentStock()).isEqualTo(30); // 20 + 10 = 30
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
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById("unknown_id");
        });
    }
}