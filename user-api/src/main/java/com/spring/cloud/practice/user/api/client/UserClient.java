package com.spring.cloud.practice.user.api.client;

import com.spring.cloud.practice.user.api.contract.UserContract;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserClient extends UserContract {
}
