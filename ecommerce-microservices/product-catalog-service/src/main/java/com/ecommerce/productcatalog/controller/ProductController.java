package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.request.ProductCreateRequest;
import com.ecommerce.productcatalog.dto.request.ProductFilterRequest;
import com.ecommerce.productcatalog.dto.request.ProductUpdateRequest;
import com.ecommerce.productcatalog.dto.response.ProductResponse;
import com.ecommerce.productcatalog.dto.response.ProductSearchSuggestionResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Ürün CRUD, filtreleme, sayfalama ve Redis Cache API'leri")
public class ProductController {

    private final ProductService productService;

    // ==========================================
    // REDIS CACHE ENDPOINTS
    // ==========================================

    @GetMapping("/top")
    @Operation(summary = "En Çok Satılan / Popüler 10 Ürün (Redis Cached)", description = "Saatte bir güncellenen veya ilk istekte cache'lenen top 10 ürünü getirir.")
    public ResponseEntity<List<ProductResponse>> getTop10Products() {
        return ResponseEntity.ok(productService.getTopProducts(10));
    }

    @GetMapping("/top-50")
    @Operation(summary = "En Popüler 50 Ürün (Redis Cached)", description = "6 saatte bir güncellenen veya ilk istekte cache'lenen top 50 ürünü getirir.")
    public ResponseEntity<List<ProductResponse>> getTop50Products() {
        return ResponseEntity.ok(productService.getTopProducts(50));
    }

    @GetMapping("/admin/cache-status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Cache Durumunu Sorgula", description = "Admin paneli için Redis cache doluluk ve kalan TTL bilgilerini döner.")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(productService.getCacheStatus());
    }

    @DeleteMapping("/admin/cache/clear")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Ürün Cache'lerini Manuel Temizle", description = "Admin tarafından Redis ürün cache'lerini anında temizler.")
    public ResponseEntity<Void> clearCache() {
        productService.clearProductCaches();
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // EXISTING PRODUCT ENDPOINTS
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
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
    @Operation(summary = "Dinamik Filtreli Ürün Listeleme", description = "Arama, kategori, marka, fiyat aralığı, pasif ürün dahil etme ve sayfalama parametrelerine göre filtreler.")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        String actualSortBy = "price".equalsIgnoreCase(sortBy) ? "price.sellingPrice" : sortBy;
        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(actualSortBy).ascending() : Sort.by(actualSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .search(search)
                .categoryId(categoryId)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .includeInactive(includeInactive)
                .build();

        return ResponseEntity.ok(productService.getFilteredProducts(filter, pageable));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Kategoriye Göre Ürünleri Listele", description = "Seçilen kategoriye ait ürünleri sayfalamalı ve filtreli listeler.")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategoryId(
            @PathVariable("categoryId") String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        String actualSortBy = "price".equalsIgnoreCase(sortBy) ? "price.sellingPrice" : sortBy;
        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(actualSortBy).ascending() : Sort.by(actualSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryId(categoryId)
                .search(search)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .includeInactive(includeInactive)
                .build();

        return ResponseEntity.ok(productService.getFilteredProducts(filter, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @Operation(summary = "Ürün Güncelle", description = "Mevcut ürün verilerini günceller.")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("id") String id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reduce-stock")
    @Operation(summary = "Ürün Stok Düşür", description = "Sipariş sonrasında ürün stoğunu belirtilen miktar kadar azaltır.")
    public ResponseEntity<Void> reduceStock(
            @PathVariable("id") String id,
            @RequestParam(name = "quantity") Integer quantity
    ) {
        productService.reduceStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/restore-stock")
    @Operation(summary = "Ürün Stok İade Et", description = "Sipariş iptali sonrasında ürün stoğunu belirtilen miktar kadar artırır.")
    public ResponseEntity<Void> restoreStock(
            @PathVariable("id") String id,
            @RequestParam(name = "quantity") Integer quantity
    ) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @Operation(summary = "Ürüne Görsel Ekle", description = "MinIO depolamasına görselleri yükler ve ürün dokümanına ekler.")
    public ResponseEntity<ProductResponse> uploadProductImages(
            @PathVariable("id") String id,
            @RequestPart("files") List<MultipartFile> files) {
        ProductResponse response = productService.addImagesToProduct(id, files);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/record-sale")
    public ResponseEntity<Void> recordSale(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int quantity) {
        productService.updateProductSalesAndScore(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Hızlı Arama Önerileri (Autocomplete)", description = "Header arama kutusu için anlık hafif ürün listesi döner.")
    public ResponseEntity<List<ProductSearchSuggestionResponse>> getSuggestions(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(productService.getSearchSuggestions(query, limit));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @Operation(summary = "Ürün Aktif/Pasif Durumunu Değiştir", description = "Ürünü müşterilere geçici olarak gizler veya tekrar görünür yapar.")
    public ResponseEntity<ProductResponse> toggleProductStatus(@PathVariable("id") String id) {
        ProductResponse response = productService.toggleProductStatus(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @Operation(summary = "Ürün Sil (Soft Delete)", description = "Ürünü veritabanından tamamen silmez, isDeleted alanını true yapar.")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}