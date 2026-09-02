package com.ecommerce.productcatalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // ==========================================
    // 1. DIRECT AKIŞ: SIPARIS & STOK TANIMLARI
    // ==========================================
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String STOCK_ROUTING_KEY = "order.created";
    public static final String STOCK_RESTORE_ROUTING_KEY = "order.cancelled";

    public static final String STOCK_QUEUE = "product.stock.deduct.queue";
    public static final String STOCK_RESTORE_QUEUE = "product.stock.restore.queue";

    // --- DLQ (Dead Letter Queue) Tanımları ---
    public static final String STOCK_DLX = "product.stock.dlx";
    public static final String STOCK_DLQ = "product.stock.deduct.dlq";
    public static final String STOCK_DLQ_ROUTING_KEY = "product.stock.dead-letter";

    // ==========================================
    // 2. FANOUT AKIŞ: CACHE INVALIDATION TANIMLARI
    // ==========================================
    public static final String CACHE_FANOUT_EXCHANGE = "cache.fanout.exchange";

    // --- Direct Exchange Bean'leri ---

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public DirectExchange stockDeadLetterExchange() {
        return new DirectExchange(STOCK_DLX);
    }

    // --- Stok Kuyruk Bean'leri ---

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(STOCK_QUEUE)
                .withArgument("x-dead-letter-exchange", STOCK_DLX)
                .withArgument("x-dead-letter-routing-key", STOCK_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue stockRestoreQueue() {
        return QueueBuilder.durable(STOCK_RESTORE_QUEUE).build();
    }

    @Bean
    public Queue stockDeadLetterQueue() {
        return QueueBuilder.durable(STOCK_DLQ).build();
    }

    // --- Stok Binding Bean'leri ---

    @Bean
    public Binding stockBinding(@Qualifier("stockQueue") Queue stockQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(stockQueue).to(orderExchange).with(STOCK_ROUTING_KEY);
    }

    @Bean
    public Binding stockRestoreBinding(@Qualifier("stockRestoreQueue") Queue stockRestoreQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(stockRestoreQueue).to(orderExchange).with(STOCK_RESTORE_ROUTING_KEY);
    }

    @Bean
    public Binding stockDeadLetterBinding(@Qualifier("stockDeadLetterQueue") Queue stockDeadLetterQueue,
                                          @Qualifier("stockDeadLetterExchange") DirectExchange stockDeadLetterExchange) {
        return BindingBuilder.bind(stockDeadLetterQueue).to(stockDeadLetterExchange).with(STOCK_DLQ_ROUTING_KEY);
    }

    // --- Fanout Exchange & Dinamik Kuyruk Bean'leri ---

    @Bean
    public FanoutExchange cacheFanoutExchange() {
        return new FanoutExchange(CACHE_FANOUT_EXCHANGE);
    }

    // Her instance ayağa kalktığında benzersiz rastgele isimli, instance kapandığında silinen kuyruk
    @Bean
    public Queue autoDeleteCacheQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding cacheFanoutBinding(@Qualifier("autoDeleteCacheQueue") Queue autoDeleteCacheQueue,
                                      @Qualifier("cacheFanoutExchange") FanoutExchange cacheFanoutExchange) {
        return BindingBuilder.bind(autoDeleteCacheQueue).to(cacheFanoutExchange);
    }

    // ==========================================
    // 3. SERIALIZATION & TEMPLATE TANIMLARI
    // ==========================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}