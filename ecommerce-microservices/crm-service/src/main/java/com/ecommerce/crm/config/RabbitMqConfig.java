package com.ecommerce.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    // Routing Keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // CRM Kuyrukları
    public static final String CRM_ORDER_CREATED_QUEUE = "crm.order.created.queue";
    public static final String CRM_ORDER_CANCELLED_QUEUE = "crm.order.cancelled.queue";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue crmOrderCreatedQueue() {
        return QueueBuilder.durable(CRM_ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue crmOrderCancelledQueue() {
        return QueueBuilder.durable(CRM_ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    public Binding crmOrderCreatedBinding(@Qualifier("crmOrderCreatedQueue") Queue queue, DirectExchange orderExchange) {
        return BindingBuilder.bind(queue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding crmOrderCancelledBinding(@Qualifier("crmOrderCancelledQueue") Queue queue, DirectExchange orderExchange) {
        return BindingBuilder.bind(queue).to(orderExchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

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