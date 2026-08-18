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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Kategori ekleme ve listeleme API'ları")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Yeni Kategori Oluştur", description = "Sisteme yeni bir kategori ekler.")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Tüm Kategorileri Getir", description = "Sistemdeki tüm kategorileri listeler.")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}
