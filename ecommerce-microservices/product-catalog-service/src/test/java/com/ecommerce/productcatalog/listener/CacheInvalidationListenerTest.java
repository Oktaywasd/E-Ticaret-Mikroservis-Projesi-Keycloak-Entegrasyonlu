package com.ecommerce.productcatalog.listener;

import com.ecommerce.productcatalog.dto.event.CacheInvalidationEvent;
import com.ecommerce.productcatalog.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationListenerTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheInvalidationListener cacheInvalidationListener;

    @Test
    @DisplayName("Cache Invalidation eventi geldiğinde popüler ürün cache'leri temizlenmelidir")
    void shouldClearCachesWhenInvalidationEventReceived() {
        // Given
        CacheInvalidationEvent event = CacheInvalidationEvent.builder()
                .cachePattern("cache:top_*")
                .triggeredBy("STOCK_REDUCED")
                .build();

        // When
        cacheInvalidationListener.handleCacheInvalidation(event);

        // Then
        verify(cacheService, times(1)).delete("cache:top_*");
        verify(cacheService, times(1)).delete("cache:top_products");
        verify(cacheService, times(1)).delete("cache:top_50_products");
    }
}