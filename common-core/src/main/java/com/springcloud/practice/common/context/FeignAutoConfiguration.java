package com.springcloud.practice.common.context;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignAutoConfiguration {

    @Bean
    public RequestInterceptor feignUserContextInterceptor() {
        return template -> {
            Long userId = UserContext.getUserId();
            if (userId != null) {
                template.header(UserContextInterceptor.HEADER_USER_ID, userId.toString());
            }
        };
    }
}