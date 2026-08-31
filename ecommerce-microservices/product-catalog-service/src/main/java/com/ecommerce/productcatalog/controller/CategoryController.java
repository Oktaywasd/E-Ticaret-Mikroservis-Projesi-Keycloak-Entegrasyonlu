package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.request.CategoryCreateRequest;
import com.ecommerce.productcatalog.dto.response.CategoryResponse;
import com.ecommerce.productcatalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Kategori ekleme, listeleme, güncelleme, silme ve cache API'ları")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Yeni Kategori Oluştur", description = "Sisteme yeni bir kategori ekler ve cache'i temizler.")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Tüm Kategorileri Getir (Redis Cached)", description = "Sistemdeki tüm kategorileri 30 dakikalık Redis cache üzerinden hızlıca listeler.")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile Kategori Getir", description = "Belirtilen ID'ye sahip kategoriyi getirir.")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Kategori Güncelle", description = "Belirtilen kategorinin bilgilerini günceller ve cache'i temizler.")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Kategori Sil", description = "Belirtilen kategoriyi sistemden siler ve cache'i temizler.")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/cache/clear")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Kategori Cache'ini Temizle", description = "Redis üzerindeki kategori önbelleğini manuel temizler.")
    public ResponseEntity<Void> clearCategoryCache() {
        categoryService.clearCategoryCache();
        return ResponseEntity.noContent().build();
    }
}