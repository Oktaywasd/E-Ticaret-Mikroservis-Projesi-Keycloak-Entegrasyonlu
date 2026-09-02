package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.config.RabbitMqConfig;
import com.ecommerce.productcatalog.dto.event.CacheInvalidationEvent;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
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
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    private static final String CACHE_TOP_PRODUCTS = "cache:top_products";
    private static final String CACHE_TOP_50_PRODUCTS = "cache:top_50_products";

    @Value("${app.cache.top-products-ttl:3600}")
    private long topProductsTtl;

    @Value("${app.cache.top-50-products-ttl:21600}")
    private long top50ProductsTtl;

    // ==========================================
    // REDIS CACHED PRODUCT FETCHING
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductResponse> getTop10Products() {
        Object cachedData = cacheService.get(CACHE_TOP_PRODUCTS);
        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, new TypeReference<List<ProductResponse>>() {});
            } catch (Exception e) {
                log.warn("Cache deserialization hatası (Top 10): {}", e.getMessage());
            }
        }

        List<Product> products = productRepository.findTop10ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        List<ProductResponse> responses = productMapper.toResponseList(products);

        cacheService.set(CACHE_TOP_PRODUCTS, responses, topProductsTtl);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getTop50Products() {
        Object cachedData = cacheService.get(CACHE_TOP_50_PRODUCTS);
        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, new TypeReference<List<ProductResponse>>() {});
            } catch (Exception e) {
                log.warn("Cache deserialization hatası (Top 50): {}", e.getMessage());
            }
        }

        List<Product> products = productRepository.findTop50ByIsActiveTrueAndIsDeletedFalseOrderByPopularityScoreDesc();
        List<ProductResponse> responses = productMapper.toResponseList(products);

        cacheService.set(CACHE_TOP_50_PRODUCTS, responses, top50ProductsTtl);
        return responses;
    }

    public List<ProductResponse> getTopProducts(int limit) {
        return limit <= 10 ? getTop10Products() : getTop50Products();
    }

    public void clearProductCaches() {
        clearProductCaches("MANUAL_OR_DATA_CHANGE");
    }

    public void clearProductCaches(String reason) {
        // 1. Yerel instance Redis önbelleklerini temizle
        cacheService.delete(CACHE_TOP_PRODUCTS);
        cacheService.delete(CACHE_TOP_50_PRODUCTS);
        log.info("Tüm ürün cache'leri temizlendi. (Sebep: {})", reason);

        // 2. Diğer tüm kopyalara Fanout Exchange üzerinden anlık sinyal gönder
        try {
            CacheInvalidationEvent event = CacheInvalidationEvent.builder()
                    .cachePattern("cache:top_*")
                    .triggeredBy(reason)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMqConfig.CACHE_FANOUT_EXCHANGE, "", event);
            log.info("Cache Invalidation Fanout eventi yayınlandı: {}", RabbitMqConfig.CACHE_FANOUT_EXCHANGE);
        } catch (Exception e) {
            log.error("Cache Invalidation Fanout mesajı gönderilirken hata oluştu: {}", e.getMessage(), e);
        }
    }

    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("top10Cached", cacheService.get(CACHE_TOP_PRODUCTS) != null);
        status.put("top10TtlRemaining", cacheService.getTtl(CACHE_TOP_PRODUCTS));
        status.put("top50Cached", cacheService.get(CACHE_TOP_50_PRODUCTS) != null);
        status.put("top50TtlRemaining", cacheService.getTtl(CACHE_TOP_50_PRODUCTS));
        return status;
    }

    /**
     * Sipariş tamamlandığında satış adedini ve popülerlik skorunu günceller.
     */
    @Transactional
    public void updateProductSalesAndScore(String productId, int quantitySold) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + productId));

        int newSales = (product.getSalesCount() != null ? product.getSalesCount() : 0) + quantitySold;
        product.setSalesCount(newSales);

        double rating = product.getRatingAverage() != null ? product.getRatingAverage() : 0.0;
        int reviews = product.getReviewCount() != null ? product.getReviewCount() : 0;
        double newScore = (newSales * 10.0) + (rating * 5.0) + (reviews * 2.0);

        product.setPopularityScore(newScore);
        product.setUpdatedDate(LocalDateTime.now());
        productRepository.save(product);

        clearProductCaches("PRODUCT_SALES_AND_SCORE_UPDATED");
    }

    // ==========================================
    // EXISTING PRODUCT CRUD & LOGIC
    // ==========================================

    public Page<ProductResponse> getFilteredProducts(ProductFilterRequest filter, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isDeleted").ne(true));

        if (!Boolean.TRUE.equals(filter.getIncludeInactive())) {
            query.addCriteria(Criteria.where("isActive").ne(false));
        }

        if (filter.getCategoryId() != null && !filter.getCategoryId().isBlank()) {
            query.addCriteria(Criteria.where("categoryId").is(filter.getCategoryId().trim()));
        }

        if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            query.addCriteria(Criteria.where("brand").regex(filter.getBrand().trim(), "i"));
        }

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String searchRegex = filter.getSearch().trim();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(searchRegex, "i"),
                    Criteria.where("brand").regex(searchRegex, "i")
            ));
        }

        if (filter.getMinPrice() != null && filter.getMaxPrice() != null) {
            query.addCriteria(Criteria.where("price.sellingPrice")
                    .gte(filter.getMinPrice().doubleValue())
                    .lte(filter.getMaxPrice().doubleValue()));
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
        product.setSalesCount(0);
        product.setPopularityScore(0.0);
        product.setCreatedDate(LocalDateTime.now());
        product.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        clearProductCaches("PRODUCT_CREATED");
        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse getProductById(String id) {
        Product product = findActiveProductById(id);
        return productMapper.toResponse(product);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllByIsDeletedFalse(pageable);
        return products.map(productMapper::toResponse);
    }

    public Page<ProductResponse> getProductsByCategoryId(String categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Kategori bulunamadı: " + categoryId);
        }
        Page<Product> products = productRepository.findAllByCategoryIdAndIsDeletedFalse(categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        Product existingProduct = findActiveProductById(id);

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException("Kategori bulunamadı: " + request.getCategoryId());
        }

        Product updatedProduct = productMapper.updateEntityFromRequest(request, existingProduct);
        updatedProduct.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(updatedProduct);
        clearProductCaches("PRODUCT_UPDATED");
        return productMapper.toResponse(savedProduct);
    }

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

        // Satış adedini ve popülerlik skorunu eşzamanlı artır
        int currentSales = product.getSalesCount() != null ? product.getSalesCount() : 0;
        int updatedSales = currentSales + quantity;
        product.setSalesCount(updatedSales);

        double rating = product.getRatingAverage() != null ? product.getRatingAverage() : 0.0;
        int reviews = product.getReviewCount() != null ? product.getReviewCount() : 0;
        double newScore = (updatedSales * 10.0) + (rating * 5.0) + (reviews * 2.0);
        product.setPopularityScore(newScore);

        product.setUpdatedDate(LocalDateTime.now());
        productRepository.save(product);

        clearProductCaches("STOCK_REDUCED");
        log.info("Stock reduced and sales updated. Product: {}, New Stock: {}, Sales: {}, Score: {}",
                id, newStock, updatedSales, newScore);
    }

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
        clearProductCaches("STOCK_RESTORED");
        log.info("Stock restored successfully for product {}. Added: {}, New Stock: {}", id, quantity, newStock);
    }

    public void deleteProduct(String id) {
        Product product = findActiveProductById(id);

        product.setIsDeleted(true);
        product.setUpdatedDate(LocalDateTime.now());
        productRepository.save(product);
        clearProductCaches("PRODUCT_DELETED");
    }

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
        clearProductCaches("PRODUCT_IMAGES_ADDED");
        return productMapper.toResponse(updatedProduct);
    }

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

    public ProductResponse toggleProductStatus(String id) {
        Product product = findActiveProductById(id);

        boolean currentStatus = product.getIsActive() == null || product.getIsActive();
        product.setIsActive(!currentStatus);
        product.setUpdatedDate(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        clearProductCaches("PRODUCT_STATUS_TOGGLED");
        log.info("Product ID: {} status toggled. New isActive state: {}", id, product.getIsActive());
        return productMapper.toResponse(savedProduct);
    }

    private Product findActiveProductById(String id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseGet(() -> productRepository.findById(id)
                        .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı ID: " + id)));
    }
}