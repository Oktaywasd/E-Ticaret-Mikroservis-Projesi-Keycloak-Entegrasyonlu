package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheSchedulerService {

    private final ProductService productService;
    private final CacheService cacheService;

    private static final String CACHE_TOP_PRODUCTS = "cache:top_products";
    private static final String CACHE_TOP_50_PRODUCTS = "cache:top_50_products";

    @Value("${app.cache.top-products-ttl:3600}")
    private long topProductsTtl;

    @Value("${app.cache.top-50-products-ttl:21600}")
    private long top50ProductsTtl;

    // Her saat başı Top 10 ürün önbelleğini veritabanından tazeleyip ısıtır
    @Scheduled(cron = "0 0 * * * *")
    public void refreshTopProductsCache() {
        log.info("Zamanlanmış görev başladı: Top 10 popüler ürün önbelleği yenileniyor...");
        try {
            // Önce mevcut önbelleği silip taze listeyi çeker
            cacheService.delete(CACHE_TOP_PRODUCTS);
            List<ProductResponse> freshProducts = productService.getTop10Products();
            log.info("Top 10 popüler ürün önbelleği başarıyla tazelendi. Ürün adedi: {}", freshProducts.size());
        } catch (Exception e) {
            log.error("Top 10 ürün önbelleği yenilenirken hata oluştu: ", e);
        }
    }

    // Her 6 saatte bir Top 50 ürün önbelleğini veritabanından tazeleyip ısıtır
    @Scheduled(cron = "0 0 */6 * * *")
    public void refreshTop50ProductsCache() {
        log.info("Zamanlanmış görev başladı: Top 50 trend ürün önbelleği yenileniyor...");
        try {
            cacheService.delete(CACHE_TOP_50_PRODUCTS);
            List<ProductResponse> freshProducts = productService.getTop50Products();
            log.info("Top 50 trend ürün önbelleği başarıyla tazelendi. Ürün adedi: {}", freshProducts.size());
        } catch (Exception e) {
            log.error("Top 50 ürün önbelleği yenilenirken hata oluştu: ", e);
        }
    }
}