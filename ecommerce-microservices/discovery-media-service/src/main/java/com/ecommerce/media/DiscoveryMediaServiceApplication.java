package com.ecommerce.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableFeignClients
@EnableSpringDataWebSupport
public class DiscoveryMediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryMediaServiceApplication.class, args);
    }
}