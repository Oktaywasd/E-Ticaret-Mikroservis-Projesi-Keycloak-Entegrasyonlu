package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.CategoryCreateRequest;
import com.ecommerce.productcatalog.dto.response.CategoryResponse;
import com.ecommerce.productcatalog.exception.AlreadyExistsException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.mapper.CategoryMapper;
import com.ecommerce.productcatalog.model.Category;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String CACHE_CATEGORIES_ALL = "cache:categories:all";

    @Value("${app.cache.categories-ttl:1800}")
    private long categoriesTtl;

    // 1. Yeni Kategori Oluşturma (Cache Invalidation)
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Bu isimde bir kategori zaten mevcut: " + request.getName());
        }

        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);

        clearCategoryCache();
        return categoryMapper.toResponse(savedCategory);
    }

    // 2. Tüm Kategorileri Getir (Önce Redis Cache, Yoksa DB)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        Object cachedData = cacheService.get(CACHE_CATEGORIES_ALL);
        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, new TypeReference<List<CategoryResponse>>() {});
            } catch (Exception e) {
                log.warn("Kategori cache deserialize edilemedi, veritabanından çekiliyor: {}", e.getMessage());
            }
        }

        List<CategoryResponse> categories = fetchAllCategoriesFromDb();
        cacheService.set(CACHE_CATEGORIES_ALL, categories, categoriesTtl);
        return categories;
    }

    // 2.1 DB'den Doğrudan Kategori Çekme (Scheduler & Cache Miss İçin)
    @Transactional(readOnly = true)
    public List<CategoryResponse> fetchAllCategoriesFromDb() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    // 3. ID ile Kategori Getirme
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + id));
        return categoryMapper.toResponse(category);
    }

    // 4. Kategori Güncelleme (Cache Invalidation)
    @Transactional
    public CategoryResponse updateCategory(String id, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + id));

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Bu isimde bir kategori zaten mevcut: " + request.getName());
        }

        category.setName(request.getName());
        Category updatedCategory = categoryRepository.save(category);

        clearCategoryCache();
        return categoryMapper.toResponse(updatedCategory);
    }

    // 5. Kategori Silme (Cache Invalidation)
    @Transactional
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kategori bulunamadı: " + id);
        }
        categoryRepository.deleteById(id);
        clearCategoryCache();
    }

    // 6. Cache Temizleme
    public void clearCategoryCache() {
        cacheService.delete(CACHE_CATEGORIES_ALL);
        log.info("Kategori cache'i ({}) başarıyla temizlendi.", CACHE_CATEGORIES_ALL);
    }
}