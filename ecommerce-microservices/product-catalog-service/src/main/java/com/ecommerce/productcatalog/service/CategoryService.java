package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.request.CategoryCreateRequest;
import com.ecommerce.productcatalog.dto.response.CategoryResponse;
import com.ecommerce.productcatalog.exception.AlreadyExistsException;
import com.ecommerce.productcatalog.exception.ResourceNotFoundException;
import com.ecommerce.productcatalog.mapper.CategoryMapper;
import com.ecommerce.productcatalog.model.Category;
import com.ecommerce.productcatalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private  final CategoryRepository categoryRepository;
    private  final CategoryMapper categoryMapper;

    public CategoryResponse createCategory(CategoryCreateRequest request) {
        // İş Kuralı: Aynı isimde kategori var mı kontrol et
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Bu isimde bir kategori zaten mevcut: " + request.getName());
        }

        // Üst kategori var mı kontrol et (eğer parentId gönderildiyse)
        if (request.getParentCategoryId() != null && !request.getParentCategoryId().isEmpty()) {
            if (!categoryRepository.existsById(request.getParentCategoryId())) {
                throw new ResourceNotFoundException("Üst kategori bulunamadı: " + request.getParentCategoryId());
            }
        }
        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(categoryMapper::toResponse).toList();
    }
}
