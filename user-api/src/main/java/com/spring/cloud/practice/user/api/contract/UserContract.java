package com.spring.cloud.practice.user.api.contract;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.spring.cloud.practice.user.api.dto.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

public interface UserContract {
    @Cached(
            area = "default",
            key = "#id",
            expire = 30,
            localExpire = 25,
            timeUnit = TimeUnit.SECONDS,
            syncLocal = true,
            cacheType = CacheType.BOTH
    )
    @GetMapping("/internal/user/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

}
