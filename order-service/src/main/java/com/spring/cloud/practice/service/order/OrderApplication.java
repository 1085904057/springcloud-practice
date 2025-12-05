package com.spring.cloud.practice.service.order;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.spring.cloud.practice.user.api.client.UserClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = UserClient.class)
@EnableMethodCache(basePackages = ("com.spring.cloud.practice"))
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}