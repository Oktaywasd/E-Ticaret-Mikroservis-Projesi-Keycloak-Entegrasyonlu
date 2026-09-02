package com.ecommerce.media.config;

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
public class ReelsRabbitMqConfig {

    public static final String REELS_EXCHANGE = "reels.exchange";
    public static final String REELS_INTERACTION_QUEUE = "reels.interaction.queue";
    public static final String REELS_INTERACTION_ROUTING_KEY = "reels.interaction";

    @Bean
    public DirectExchange reelsExchange() {
        return new DirectExchange(REELS_EXCHANGE);
    }

    @Bean
    public Queue reelsInteractionQueue() {
        return QueueBuilder.durable(REELS_INTERACTION_QUEUE).build();
    }

    @Bean
    public Binding reelsInteractionBinding(@Qualifier("reelsInteractionQueue") Queue queue,
                                           DirectExchange reelsExchange) {
        return BindingBuilder.bind(queue).to(reelsExchange).with(REELS_INTERACTION_ROUTING_KEY);
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