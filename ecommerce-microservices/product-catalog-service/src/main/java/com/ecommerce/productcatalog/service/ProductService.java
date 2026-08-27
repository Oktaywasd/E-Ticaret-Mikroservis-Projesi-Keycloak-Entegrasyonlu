package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.request.ProductFilterRequest;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.dto.response.ProductSearchSuggestionResponse;
import com.ecommerce.productcatalog.exception.AlreadyExistsException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.mapper.ProductMapper;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final MongoTemplate mongoTemplate;

    // 0. Dinamik Filtreleme ve Sayfalama (Server-Side)
    public Page<ProductResponse> getFilteredProducts(ProductFilterRequest filter, Pageable pageable) {
        Query query = new Query();

        // 0.1. Silinmemiş Ürünler (Zorunlu)
        query.addCriteria(Criteria.where("isDeleted").ne(true));

        // 0.2. Aktiflik Durumu: Admin isteği değilse (includeInactive != true) sadece aktif ürünleri getir
        if (!Boolean.TRUE.equals(filter.getIncludeInactive())) {
            query.addCriteria(Criteria.where("isActive").ne(false));
        }

        // 0.3. Kategori Filtresi
        if (filter.getCategoryId() != null && !filter.getCategoryId().isBlank()) {
            query.addCriteria(Criteria.where("categoryId").is(filter.getCategoryId().trim()));
        }

        // 0.4. Marka Filtresi (Regex - Case Insensitive)
        if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            query.addCriteria(Criteria.where("brand").regex(filter.getBrand().trim(), "i"));
        }

        // 0.5. İsim veya Marka Arama
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String searchRegex = filter.getSearch().trim();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(searchRegex, "i"),
                    Criteria.where("brand").regex(searchRegex, "i")
            ));
        }

        if (filter.getMinPrice() != null && filter.getMaxPrice() != null) {
            query.addCriteria(Criteria.where("price.sellingPrice")
                    .gte(filter.getMinPrice().doubleValue())  // <-- Double gönderiliyor
                    .lte(filter.getMaxPrice().doubleValue())); // <-- Double gönderiliyor
        } else if (filter.getMinPrice() != null) {
            query.addCriteria(Criteria.where("price.sellingPrice")
                    .gte(filter.getMinPrice().doubleValue()));
        } else if (filter.getMaxPrice() != null) {
            query.addCriteria(Criteria.where("price.sellingPrice")
                    .lte(filter.getMaxPrice().doubleValue()));
        }

        long total = mongoTemplate.count(query, Product.class);

        query.with(pageable);
        List<Product> products = mongoTemplate.find(query, Product.class);

        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    // 1. Ürün Oluşturma
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsByProductCodeAndIsDeletedFalse(request.getProductCode())) {
            throw new AlreadyExistsException("Bu ürün kodu ile aktif bir ürün zaten var: " + request.getProductCode());
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException("Belirtilen kategori bulunamadı: " + request.getCategoryId());
        }

        Product product = productMapper.toEntity(request);
        product.setIsDeleted(false);
        product.setIsActive(true);
        product.setCreatedDate(LocalDateTime.now());
        product.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    // 2. ID ile Ürün Getirme
    public ProductResponse getProductById(String id) {
        Product product = findActiveProductById(id);
        return productMapper.toResponse(product);
    }

    // 3. Tüm Ürünleri Sayfalamalı Listeleme (Admin ve genel kullanım)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllByIsDeletedFalse(pageable);
        return products.map(productMapper::toResponse);
    }

    // 4. Kategoriye Göre Ürün Listeleme
    public Page<ProductResponse> getProductsByCategoryId(String categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Kategori bulunamadı: " + categoryId);
        }
        Page<Product> products = productRepository.findAllByCategoryIdAndIsDeletedFalse(categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    // 5. Ürün Güncelleme
    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        Product existingProduct = findActiveProductById(id);

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException("Kategori bulunamadı: " + request.getCategoryId());
        }

        Product updatedProduct = productMapper.updateEntityFromRequest(request, existingProduct);
        updatedProduct.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(updatedProduct);
        return productMapper.toResponse(savedProduct);
    }

    // 6. Stok Düşürme (GÜVENLİ VE LOGLU HALE GETİRİLDİ)
    @Transactional
    public void reduceStock(String id, Integer quantity) {
        log.info("Attempting to reduce stock for product ID: {} by quantity: {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok düşülecek ürün bulunamadı ID: " + id));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AlreadyExistsException("Silinmiş ürünün stoğu düşülemez ID: " + id);
        }

        if (product.getStock() == null) {
            log.error("Stock object is null for product ID: {}", id);
            throw new AlreadyExistsException("Ürünün stok yapısı bulunamadı ID: " + id);
        }

        Integer currentStock = product.getStock().getCurrentStock();
        if (currentStock == null || currentStock < quantity) {
            log.error("Insufficient stock! Available: {}, Requested: {}", currentStock, quantity);
            throw new AlreadyExistsException("Yetersiz stok! Mevcut: " + (currentStock != null ? currentStock : 0) + ", İstenen: " + quantity);
        }

        int newStock = currentStock - quantity;
        product.getStock().setCurrentStock(newStock);
        product.setUpdatedDate(LocalDateTime.now());

        productRepository.save(product);
        log.info("Stock reduced successfully for product {}. Quantity: {}, New Stock: {}", id, quantity, newStock);
    }

    // 6.1 Sipariş İptalinde Stok İade Etme (GÜVENLİ VE LOGLU)
    @Transactional
    public void restoreStock(String id, Integer quantity) {
        log.info("Attempting to restore stock for product ID: {} by quantity: {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok iade edilecek ürün bulunamadı ID: " + id));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AlreadyExistsException("Silinmiş ürünün stoğu artırılamaz ID: " + id);
        }

        if (product.getStock() == null) {
            log.error("Stock object is null for product ID: {}", id);
            throw new ResourceNotFoundException("Ürünün stok yapısı bulunamadı ID: " + id);
        }

        Integer currentStock = product.getStock().getCurrentStock();
        int safeCurrentStock = (currentStock != null) ? currentStock : 0;
        int newStock = safeCurrentStock + quantity;

        product.getStock().setCurrentStock(newStock);
        product.setUpdatedDate(LocalDateTime.now());

        productRepository.save(product);
        log.info("Stock restored successfully for product {}. Added: {}, New Stock: {}", id, quantity, newStock);
    }

    // 7. Ürün Silme (Soft Delete)
    public void deleteProduct(String id) {
        Product product = findActiveProductById(id);

        product.setIsDeleted(true);
        product.setUpdatedDate(LocalDateTime.now());
        productRepository.save(product);
    }

    // 8. Görsel Ekleme
    public ProductResponse addImagesToProduct(String productId, List<MultipartFile> files) {
        Product product = findActiveProductById(productId);

        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String imageUrl = fileStorageService.uploadImage(file);
                product.getImageUrls().add(imageUrl);
            }
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    // 9. Anlık Arama Önerileri (Autocomplete)
    public List<ProductSearchSuggestionResponse> getSearchSuggestions(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 10));
        List<Product> products = productRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCaseAndIsDeletedFalse(query.trim(), query.trim(), pageRequest);

        return products.stream()
                .filter(p -> p.getIsActive() == null || p.getIsActive())
                .map(p -> {
                    BigDecimal price = (p.getPrice() != null) ? p.getPrice().getSellingPrice() : null;
                    String firstImage = (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) ? p.getImageUrls().get(0) : null;

                    return ProductSearchSuggestionResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .brand(p.getBrand())
                            .price(price)
                            .imageUrl(firstImage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 10. Admin Ürün Aktif/Pasif Durum Değiştirme (Toggle Status)
    public ProductResponse toggleProductStatus(String id) {
        Product product = findActiveProductById(id);

        boolean currentStatus = product.getIsActive() == null || product.getIsActive();
        product.setIsActive(!currentStatus);
        product.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        log.info("Product ID: {} status toggled. New isActive state: {}", id, product.getIsActive());
        return productMapper.toResponse(savedProduct);
    }

    // Yardımcı Metod: Ürünü bul, yoksa genel findById dene
    private Product findActiveProductById(String id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseGet(() -> productRepository.findById(id)
                        .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı ID: " + id)));
    }
}