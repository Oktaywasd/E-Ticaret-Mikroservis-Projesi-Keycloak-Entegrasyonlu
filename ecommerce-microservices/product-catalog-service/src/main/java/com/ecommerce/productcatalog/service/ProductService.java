package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.exception.AlreadyExistsException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.mapper.ProductMapper;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.ecommerce.productcatalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

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

    // 3. Tüm Ürünleri Sayfalamalı Listeleme
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

    // 6. YENİ EKLENEN İŞ MANTIĞI: Stok Düşürme (GÜVENLİ VE LOGLU HALE GETİRİLDİ)
    @Transactional
    public void reduceStock(String id, Integer quantity) {
        log.info("Attempting to reduce stock for product ID: {} by quantity: {}", id, quantity);

        // Standart findById ile de fallback yaparak urunu kesin buluyoruz
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

    // 7. Ürün Silme (Soft Delete)
    public void deleteProduct(String id) {
        Product product = findActiveProductById(id);

        product.setIsDeleted(true);
        product.setUpdatedDate(LocalDateTime.now());
        productRepository.save(product);
    }

    // Yardımcı Metod: Ürünü bul, yoksa genel findById dene
    private Product findActiveProductById(String id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseGet(() -> productRepository.findById(id)
                        .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı ID: " + id)));
    }
}