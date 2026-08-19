package com.ecommerce.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DiscoveryMediaServiceApplication {

	public static void main(String[] args) {

        SpringApplication.run(DiscoveryMediaServiceApplication.class, args);
	}
}
