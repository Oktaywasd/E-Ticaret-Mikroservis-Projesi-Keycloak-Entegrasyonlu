package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Ürün CRUD, filtreleme ve sayfalama API'leri")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Yeni Ürün Oluştur", description = "Sisteme yeni bir ürün kaydeder.")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile Ürün Detayı Getir", description = "Belirtilen ID'ye sahip ürünün detaylarını döner.")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") String id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm Ürünleri Sayfalamalı Listele", description = "Silinmemiş ürünleri sayfalama ve sıralama desteği ile getirir.")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Kategoriye Göre Ürünleri Listele", description = "Seçilen kategoriye ait ürünleri sayfalamalı listeler.")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategoryId(
            @PathVariable("categoryId") String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Ürün Güncelle", description = "Mevcut ürün verilerini günceller.")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("id") String id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    // GÜNCELLENEN ENDPOINT: Parametre isimleri açıkça tanımlandı
    @PutMapping("/{id}/reduce-stock")
    @Operation(summary = "Ürün Stok Düşür", description = "Sipariş sonrasında ürün stoğunu belirtilen miktar kadar azaltır.")
    public ResponseEntity<Void> reduceStock(
            @PathVariable("id") String id,
            @RequestParam(name = "quantity") Integer quantity
    ) {
        productService.reduceStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ürün Sil (Soft Delete)", description = "Ürünü veritabanından tamamen silmez, isDeleted alanını true yapar.")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}