package com.springcloud.practice.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean("redissonClient0")
    public RedissonClient redissonClient0() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(0);
        return Redisson.create(config);
    }

    @Bean("redissonClient1")
    public RedissonClient redissonClient1() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(1);
        return Redisson.create(config);
    }
}
