package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.config.RabbitMqConfig;
import com.ecommerce.productcatalog.dto.event.CacheInvalidationEvent;
import com.ecommerce.productcatalog.model.Product;
import com.ecommerce.productcatalog.model.Stock;
import com.ecommerce.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceFanoutTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("reduceStock() başarılı olduğunda Cache Invalidation Fanout mesajı RabbitMQ'ya iletilmeli")
    void shouldBroadcastFanoutEvent_whenStockReduced() {
        // Arrange
        String productId = "prod-123";
        int initialStock = 20;
        int quantityToReduce = 2;

        Product product = new Product();
        product.setId(productId);
        product.setIsDeleted(false);
        product.setSalesCount(5);

        Stock stock = new Stock();
        stock.setCurrentStock(initialStock);
        product.setStock(stock);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productService.reduceStock(productId, quantityToReduce);

        // Assert
        assertThat(product.getStock().getCurrentStock()).isEqualTo(18);

        // RabbitTemplate çağrısını yakala
        ArgumentCaptor<CacheInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(CacheInvalidationEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.CACHE_FANOUT_EXCHANGE),
                eq(""),
                eventCaptor.capture()
        );

        CacheInvalidationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getTriggeredBy()).isEqualTo("STOCK_REDUCED");
        assertThat(publishedEvent.getCachePattern()).isEqualTo("cache:top_*");
    }

    @Test
    @DisplayName("restoreStock() başarılı olduğunda Cache Invalidation Fanout mesajı STOCK_RESTORED sebebiyle iletilmeli")
    void shouldBroadcastFanoutEvent_whenStockRestored() {
        // Arrange
        String productId = "prod-123";
        Product product = new Product();
        product.setId(productId);
        product.setIsDeleted(false);

        Stock stock = new Stock();
        stock.setCurrentStock(10);
        product.setStock(stock);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productService.restoreStock(productId, 5);

        // Assert
        assertThat(product.getStock().getCurrentStock()).isEqualTo(15);

        ArgumentCaptor<CacheInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(CacheInvalidationEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.CACHE_FANOUT_EXCHANGE),
                eq(""),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue().getTriggeredBy()).isEqualTo("STOCK_RESTORED");
    }
}