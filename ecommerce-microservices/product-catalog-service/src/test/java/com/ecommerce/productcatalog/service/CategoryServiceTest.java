package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.response.CategoryResponse;
import com.ecommerce.productcatalog.mapper.CategoryMapper;
import com.ecommerce.productcatalog.model.Category;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category mockCategory;
    private CategoryResponse mockResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(categoryService, "categoriesTtl", 1800L);

        mockCategory = Category.builder()
                .id("cat-123")
                .name("Elektronik")
                .build();

        mockResponse = CategoryResponse.builder()
                .id("cat-123")
                .name("Elektronik")
                .build();
    }

    @Test
    @DisplayName("Cache HIT: Kategoriler Redis'te varsa DB sorgusu atılmamalı")
    void shouldReturnCategoriesFromCacheWhenHit() {
        // Arrange
        List<CategoryResponse> cachedList = List.of(mockResponse);
        when(cacheService.get("cache:categories:all")).thenReturn(cachedList);
        when(objectMapper.convertValue(eq(cachedList), any(TypeReference.class))).thenReturn(cachedList);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Elektronik");

        verify(cacheService, times(1)).get("cache:categories:all");
        verify(categoryRepository, never()).findAll();
    }

    @Test
    @DisplayName("Cache MISS: Kategoriler Redis'te yoksa DB'den çekilip Redis'e yazılmalı")
    void shouldFetchFromDbAndSetCacheWhenMiss() {
        // Arrange
        when(cacheService.get("cache:categories:all")).thenReturn(null);
        when(categoryRepository.findAll()).thenReturn(List.of(mockCategory));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(mockResponse);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Elektronik");

        verify(cacheService, times(1)).get("cache:categories:all");
        verify(categoryRepository, times(1)).findAll();
        verify(categoryMapper, times(1)).toResponse(any(Category.class));
        verify(cacheService, times(1)).set(eq("cache:categories:all"), anyList(), eq(1800L));
    }
}